<?php

// http://stackoverflow.com/questions/4191834/custom-mvc-how-to-implement-a-render-function-for-the-controller-so-the-view-ca
class Epub_View {
	function render($file, $variables = array()) {
		extract($variables);

		ob_start();
		include $file;
		$renderedView = ob_get_clean();

		return $renderedView;
	}
}

