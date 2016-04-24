package com.mych.cloudgameclient.webview;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.InputType;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.EditText;

public class JavascriptBridge {
private static final String NAME_KEY = "Name";
    
    SharedPreferences sharedPref;
    Context context;
    WebView webView;

    public JavascriptBridge(Context ctx, SharedPreferences preferences, WebView wv) {
        sharedPref = preferences;
        context = ctx;
        webView = wv;
    }
    
    @JavascriptInterface
    public void playGame(){
    	Intent in = new Intent();  
        in.setClassName(context, "com.mych.cloudgameclient.decoder.PlayerActivity" );  
        context.startActivity(in);
    }
    
    @JavascriptInterface
    public void inputServerAddress(){
    	AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("input android host address");

        // Set up the input
        final EditText input = new EditText(context);
        // Specify the type of input expected
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("confirm", new DialogInterface.OnClickListener() { 
            @Override
            public void onClick(DialogInterface dialog, int which) {
                changeName(input.getText().toString());
            }
        });
        builder.setNegativeButton("cancle", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
    
    @JavascriptInterface
    public void askForName() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("What is your name?");

        // Set up the input
        final EditText input = new EditText(context);
        // Specify the type of input expected
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
            @Override
            public void onClick(DialogInterface dialog, int which) {
                changeName(input.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    protected void changeName(String name) {
        setName(name);
        webView.loadUrl("javascript:updateGreeting()");
    }

    protected void setName(String name) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(NAME_KEY, name);
        editor.commit();
    }
    
    @JavascriptInterface
    public String getName() {
        return sharedPref.getString(NAME_KEY, null);
    }
    
    @JavascriptInterface
    public String getHostAddress() {
        return sharedPref.getString(NAME_KEY, null);
    }
}
