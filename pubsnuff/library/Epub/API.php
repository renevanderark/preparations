<?php

require_once "Epub/Reader.php";

class Epub_API {
	protected $_epubFile = "";
	protected $_params = array();
	protected $_pubdir;
	protected $_action;
	private $_zipAccess = "";
	private $_format = "php";
	private $reader = false;

	private static $_CONTENTTYPES = array(
		"php" => "text/plain",
		"text" => "text/plain",
		"html" => "text/html",
		"xhtml" => "text/html",
		"json" => "application/json",
		"xml" => "text/xml",
		"jpg" => "image/jpeg",
		"jpeg" => "image/jpeg",
		"png" => "image/png",
		"svg" => "image/svg+xml",
		"css" => "text/css"
	);

	public function Epub_API($route, $getParams = array(), $pubdir = ".") {
		$this->_params = $getParams;
		$this->_pubdir = $pubdir;
		$this->_initPaths($route);
		$this->_format = isset($getParams["format"]) ? $getParams["format"] : "php";
		$this->_initReader();
	}

	protected function _setContentType($format = "json") {
		$this->_setHeader('Content-type: ' . (isset(self::$_CONTENTTYPES[$format]) ? self::$_CONTENTTYPES[$format] : self::$_CONTENTTYPES["php"]) . '; charset=utf-8');
	}

	protected function _initReader() {
		try {
			$this->reader = new Epub_Reader($this->_pubdir, $this->_epubFile);
		} catch(Exception $e) {
			die($e);
		}
	}

	protected function _initPaths($route) {
		$parts = explode('/', preg_replace("/\?.+$/", "", $route));
		$this->_action = array_shift($parts);
		$this->_epubFile = urldecode(array_shift($parts));
		$this->_zipAccess = implode("/", $parts);
	}

	protected function _getParam($key, $default) {
		return isset($this->_params[$key]) && !($this->_params[$key] === '') ? $this->_params[$key] : $default;
	}

	protected function _setHeader($str) {
		header($str);
	}

	public function execute() {
		try {
			$action = $this->_action . "Action";
			if (method_exists($this, $action)) {
				return $this->$action();
			}
			$this->_setHeader('HTTP/1.0 404 Not Found');
			die("Unknown action: $action");
		} catch(Exception $e) {
			$this->_setHeader('HTTP/1.0 500 Internal Server Error');
			die('HTTP/1.0 500 Internal Server Error');
		}
	}

	public function tocAction() {
		switch($this->_format) {
			case "json": $this->_setContentType($this->_format); return json_encode($this->reader->getToc()); break;
			default: return $this->reader->getToc();
		}
	}

	public function coverpageAction() {
		switch($this->_format) {
			case "json": $this->_setContentType($this->_format); return json_encode($this->reader->getCoverpage()); break;
			default: return $this->reader->getCoverpage();
		}
	}

	public function spineAction() {
		switch($this->_format) {
			case "json": $this->_setContentType($this->_format); return json_encode($this->reader->getSpine()); break;
			default: return $this->reader->getSpine();
		}
	}

	public function serveAction() {
		$this->_format = strtolower(preg_replace("/^.+\.([a-z]+)$/", "$1", $this->_zipAccess));
		$this->_setContentType($this->_format);
		return $this->reader->getFile($this->_zipAccess);
	}
}
?>
