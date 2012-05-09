package com.ryanramage.mobilegarden;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.content.Context;
import android.content.Intent;

import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.Window;


import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;

import android.graphics.Bitmap;

import android.net.Uri;


import com.couchbase.android.CouchbaseMobile;
import com.couchbase.android.ICouchbaseDelegate;

public class MobileFutonActivity extends Activity {

	private final MobileFutonActivity self = this;
	protected static final String TAG = "CouchAppActivity";

	private CouchbaseMobile couch;
	private ServiceConnection couchServiceConnection;
	private WebView webView;
        protected Context context;
        protected final static int FILECHOOSER_RESULTCODE = 1;

        private String gardenUrl;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
                getWindow().requestFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.main);                
		startCouch();
	}

	@Override
	public void onRestart() {
		super.onRestart();
		startCouch();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			unbindService(couchServiceConnection);
		} catch (IllegalArgumentException e) {
		}
	}

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.garden_menu, menu);
            return true;
        }
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            // Handle item selection
            switch (item.getItemId()) {
                case R.id.home:
                    webView.loadUrl(gardenUrl);
                    return true;
                case R.id.launch_browser:
                    launchBrowser();
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }
	private final ICouchbaseDelegate mCallback = new ICouchbaseDelegate() {
		@Override
		public void couchbaseStarted(String host, int port) {

			String url = "http://" + host + ":" + Integer.toString(port) + "/";
		    String ip = getLocalIpAddress();
		    String param = (ip == null) ? "" : "?ip=" + ip;

		    try {
				couch.installDatabase("dashboard.couch");
			} catch (IOException e) {
				e.printStackTrace();
			}

                        gardenUrl = url + "dashboard/_design/dashboard/_rewrite/";
			launchFuton(gardenUrl + param);
		}

		@Override
		public void exit(String error) {
			Log.v(TAG, error);
			couchError();
		}
	};


	private void startCouch() {
                context = getBaseContext();
		couch = new CouchbaseMobile(context, mCallback);

		try {
			couch.copyIniFile("mobilefuton.ini");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		couchServiceConnection = couch.startCouchbase();
	}

	private void couchError() {
		AlertDialog.Builder builder = new AlertDialog.Builder(self);
		builder.setMessage("Unknown Error")
				.setPositiveButton("Try Again?",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								startCouch();
							}
						})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								self.moveTaskToBack(true);
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void launchFuton(String url) {

                setProgressBarVisibility(true);
                setProgress(1000);


		webView = new WebView(this);
		webView.setWebChromeClient(new WebChromeClient());
		webView.setWebViewClient(new CustomWebViewClient());
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		webView.getSettings().setDomStorageEnabled(true);

		webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
                webView.getSettings().setBuiltInZoomControls(true);
		webView.requestFocus(View.FOCUS_DOWN);
                webView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                            case MotionEvent.ACTION_UP:
                                if (!v.hasFocus()) {
                                    v.requestFocus();
                                }
                                break;
                        }
                        return false;
                    }
                });

		setContentView(webView);
		webView.loadUrl(url);
	};

            private class CustomWebViewClient extends WebViewClient {

                @Override
                public void onPageFinished(WebView view, String url) {
                    self.setProgress(10000);
                    self.setProgressBarVisibility(false);
                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    self.setProgress(1200);
                    self.setProgressBarVisibility(true);
                }

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}

	}


        protected class CustomWebChromeClient extends WebChromeClient
        {

               @Override
               public void onProgressChanged(WebView view, int progress) {
                 // Activities and WebViews measure progress with different scales.
                 // The progress meter will automatically disappear when we reach 100%

                   int progress_total = progress * 1000;


                   self.setProgress(progress_total);
               }
            
            // For Android 3.0+
            public void openFileChooser( ValueCallback<Uri> uploadMsg, String acceptType )
            {

                Object mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                if (acceptType == null) {
                    acceptType = "*/*";
                }
                i.setType(acceptType);
                startActivityForResult( Intent.createChooser( i, "File Chooser" ), FILECHOOSER_RESULTCODE );
            }

            // For Android < 3.0
            public void openFileChooser( ValueCallback<Uri> uploadMsg )
            {
                openFileChooser( uploadMsg, "*/*" );
            }
        }

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
	    	webView.goBack();
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}

	public String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {
			ex.printStackTrace();
		}
		return null;
	}

        private void launchBrowser() {
            Uri uri = Uri.parse(webView.getUrl());
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
            
        }
}