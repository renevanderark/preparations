<?php

require_once 'Epub/API.php';
require_once 'Epub/View.php';

class Epub_App extends Epub_API {
	private $_view;

	public function Epub_App($route, $getParams = array(), $pubdir = ".") {
		$this->_pubdir = $pubdir;
		$this->_params = $getParams;
		$this->_initPaths($route);
		$this->_view = new Epub_View();
	}

	public function indexAction() {
		$files = array();
		$handle = opendir($this->_pubdir);
		while (false !== ($entry = readdir($handle))) {
			if($entry === '.' || $entry === '..') { continue; }
			$files[] = $entry;
		}
		return $this->_view->render("views/index.phtml", array("files" => $files));
	}

	public function showAction() {
		$this->_initReader();
		$toc = $this->tocAction();
		$coverPage = $this->coverpageAction();
		$spine = $this->spineAction();
		$epub = $this->_epubFile;
		return $this->_view->render("views/show.phtml", array(
			"toc" => $toc,
			"epub" => $epub,
			"spine" => $spine,
			"coverPage" => $coverPage
		));
	}

	public function downloadAction() {
		header("content-type: application/epub+zip");
		readfile(PUB_DIR . "/" . $this->_epubFile);
	}
}
