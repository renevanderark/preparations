<?php

class Epub_Reader {
	private $filepath;
	private $zip = false;
	private $container = false;
	private $opf = false;
	private $opfRoot = "";

	public function Epub_Reader($path, $file) {
		$this->filepath = "$path/$file";
		if(!is_readable($this->filepath)) { throw new Exception("Cannot read file: " . $this->filepath); }

		$this->_init();
	}

	private function _init() {
		$this->zip = new ZipArchive();
		$this->zip->open($this->filepath);
		$fp = $this->zip->getStream("META-INF/container.xml");
		$this->container = simplexml_load_string(stream_get_contents($fp));
		$this->container->registerXPathNamespace("cont","urn:oasis:names:tc:opendocument:xmlns:container");
		$rootfile = $this->container->xpath("//cont:container/cont:rootfiles/cont:rootfile/@full-path");
		fclose($fp);
		$opfFile = (string) $rootfile[0];
		if(preg_match("/\//", $opfFile)) {
			$this->opfRoot = preg_replace("/\/.+$/", "", $opfFile) . "/";
		} else {
			$this->opfRoot = "";
		}
		$fp = $this->zip->getStream($opfFile);
		$this->opf = simplexml_load_string(stream_get_contents($fp));
		$this->opf->registerXPathNamespace("opf", "http://www.idpf.org/2007/opf");
		fclose($fp);
	}

	private function _addNavPoint($navPoint, &$navPoints) {
		$navPoint->registerXPathNamespace("ncx", "http://www.daisy.org/z3986/2005/ncx/");
		$label = $navPoint->xpath("ncx:navLabel/ncx:text/text()");
		$src = $navPoint->xpath("ncx:content/@src");
		$aNavPoint = array(
			"label" => isset($label[0]) ? (string) $label[0] : count($navPoints),
			"src" => (string) $src[0],
		);

		foreach($navPoint->xpath("ncx:navPoint") as $navPoint) {
			if(!isset($aNavPoint["navPoints"])) { $aNavPoint["navPoints"] = array(); }
			$this->_addNavPoint($navPoint, $aNavPoint["navPoints"]);
		}
		$navPoints[] = $aNavPoint;
	}

	private function _parseToc() {
		$title = $this->toc->xpath("//ncx:docTitle/ncx:text/text()");
		
		$toc = array(
			"title" => isset($title[0]) ? (string) $title[0] : "", 
			"navPoints" => array()
		);
		foreach($this->toc->xpath("//ncx:navMap/ncx:navPoint") as $navPoint) {
			$this->_addNavPoint($navPoint, $toc["navPoints"]);
		}

		return $toc;
	}

	public function getToc() {
		$toc = array();
		$tocRef = $this->opf->xpath("//opf:item[@media-type='application/x-dtbncx+xml']/@href");
		if(!isset($tocRef[0])) { return array("title" => "no table of contents", "navPoints" => array()) /* TODO: failover */; }
		$fp = $this->zip->getStream($this->opfRoot . ((string) $tocRef[0]));
		$this->toc = simplexml_load_string(stream_get_contents($fp));
		$this->toc->registerXPathNamespace("ncx", "http://www.daisy.org/z3986/2005/ncx/");
		fclose($fp);

		return $this->_parseToc();
	}

	public function getCoverpage() {
		$coverRef = $this->opf->xpath("//opf:spine/opf:itemref/@idref");
		$coverId = (string) $coverRef[0];
		$coverPage = $this->opf->xpath("//opf:manifest/opf:item[@id='$coverId']/@href");
		return (string) $coverPage[0];
	}

	public function getSpine() {
		$spine = array();
		$itemRefs = $this->opf->xpath("//opf:spine/opf:itemref/@idref");
		foreach($itemRefs as $itemRef) {
			$itemId = (string) $itemRef;
			$itemSrc = $this->opf->xpath("//opf:manifest/opf:item[@id='$itemId']/@href");
			$spine[] = (string) $itemSrc[0];
		}
		return $spine;
	}

	public function getFile($fileRef) {
		$fp = $this->zip->getStream($this->opfRoot . $fileRef);
		$data = stream_get_contents($fp);
		fclose($fp);
		return $data;
	}

	function __destruct() {
		if($this->zip !== false) { $this->zip->close(); }
	}
}


?>
