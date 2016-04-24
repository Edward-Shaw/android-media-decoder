package com.mych.cloudgameclient.webview;

import com.mych.cloudgameclient.android.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.webkit.WebView;

public class WebViewActivity extends Activity {

	private WebView webView;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.webview);

		webView = (WebView) findViewById(R.id.webView1);
		webView.getSettings().setJavaScriptEnabled(true);
		JavascriptBridge jsb = new JavascriptBridge(this, getPreferences(Context.MODE_PRIVATE), webView);
		webView.addJavascriptInterface(jsb, "JavaCode");
		//webView.loadUrl("http://www.google.com");

		String customHtml = "<html><body><h1>Hello, WebView</h1></body></html>";
		//webView.loadData(customHtml, "text/html", "UTF-8");
		webView.loadUrl("file:///android_asset/index.html");
		
	}

}