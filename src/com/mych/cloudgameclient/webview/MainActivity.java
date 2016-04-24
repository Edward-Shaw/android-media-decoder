package com.mych.cloudgameclient.webview;

import com.mych.cloudgameclient.android.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;

public class MainActivity extends Activity {

	private Button button;

	@SuppressLint("SetJavaScriptEnabled")
	public void onCreate(Bundle savedInstanceState) {
		final Context context = this;

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		WebView myWebView = (WebView) findViewById(R.id.webView1);

        myWebView.setWebChromeClient(new WebChromeClient());
        myWebView.getSettings().setJavaScriptEnabled(true);
        
        JavascriptBridge jsb = new JavascriptBridge(this, getPreferences(Context.MODE_PRIVATE), myWebView);

        myWebView.addJavascriptInterface(jsb, "JavaCode");

        myWebView.loadUrl("file:///android_asset/index.html");
		
		button = (Button) findViewById(R.id.buttonUrl);
		
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				Intent intent = new Intent(context, WebViewActivity.class);
				startActivity(intent);
			}

		});

	}

}