<?php

defined('APPLICATION_ROOT')
	|| define('APPLICATION_ROOT', realpath(dirname(__FILE__) . '/../'));

define('PUB_DIR', APPLICATION_ROOT . DIRECTORY_SEPARATOR . "../epub-gen/books");

defined('LIBRARY_PATH')
	|| define('LIBRARY_PATH', APPLICATION_ROOT . DIRECTORY_SEPARATOR . 'library' . DIRECTORY_SEPARATOR);

set_include_path(implode(PATH_SEPARATOR, array(
	APPLICATION_ROOT . DIRECTORY_SEPARATOR . 'library' . DIRECTORY_SEPARATOR,	
	get_include_path(),
)));

$path = isset($_SERVER['UNENCODED_URL']) ? $_SERVER['UNENCODED_URL'] : $_SERVER['REQUEST_URI'];


if (preg_match("/^\/epub\/(.+)/", $path, $m)) {
	require_once 'Epub/API.php';
	$api = new Epub_API($m[1], $_GET, PUB_DIR);
	echo $api->execute();
	exit();
} else {
	preg_match("/^\/(.+)/", $path, $m);
	require_once('Epub/App.php');
	if(!isset($m[1])) { $m[1] = "index"; }
	$app = new Epub_App($m[1], $_GET, PUB_DIR);
	echo $app->execute();
	
}
