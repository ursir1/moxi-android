package com.moxi.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private lateinit var fullscreenContainer: FrameLayout
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val MOXI_WEB_URL = "https://moxi-5n1.pages.dev/#/?streamingServerUrl=http%3A%2F%2F127.0.0.1%3A11470%2F"
    }

    inner class AndroidBridge {
        @android.webkit.JavascriptInterface
        fun playInExternalPlayer(streamUrl: String) {
            runOnUiThread {
                try {
                    val cleanUrl = streamUrl.trim().removePrefix("vlc://")
                    val videoIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(cleanUrl), "video/*")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(Intent.createChooser(videoIntent, "Play video with..."))
                } catch (e: Exception) {
                    Timber.e(e, "Failed to launch external player for: $streamUrl")
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Safely launch background streaming server so crashes never kill the UI
        startLocalStreamingServerSafely()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val rootLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFF090D0C.toInt())
        }

        fullscreenContainer = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
            setBackgroundColor(0xFF000000.toInt())
        }

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFF090D0C.toInt())
        }

        rootLayout.addView(webView)
        rootLayout.addView(fullscreenContainer)
        setContentView(rootLayout)

        configureWebView()
        setupBackNavigation()

        if (savedInstanceState == null) {
            webView.loadUrl(MOXI_WEB_URL)
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    private fun startLocalStreamingServerSafely() {
        scope.launch(Dispatchers.IO) {
            try {
                val app = application as? MainApplication ?: return@launch
                Timber.d("Starting native streaming server...")
                app.container.serverController.start()
                Timber.d("Native streaming server started successfully")
            } catch (t: Throwable) {
                // Catch all errors (including UnsatisfiedLinkError, NoClassDefFoundError)
                Timber.e(t, "Non-fatal: could not start streaming server in background")
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        val defaultUserAgent = settings.userAgentString
        settings.userAgentString = "$defaultUserAgent MoxiAndroid/${BuildConfig.VERSION_NAME}"

        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                val uri = request.url
                val scheme = uri.scheme?.lowercase() ?: ""

                // Handle external player schemes: vlc, intent, market, etc.
                if (scheme == "vlc" || scheme == "intent" || scheme == "market") {
                    return try {
                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        startActivity(intent)
                        true
                    } catch (e: Exception) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            startActivity(intent)
                            true
                        } catch (t: Throwable) {
                            false
                        }
                    }
                }

                // If user clicks a direct media/playlist file intended for an external player
                if (url.endsWith(".m3u8") || url.endsWith(".mkv") || url.endsWith(".mp4") || url.contains("/playback/")) {
                    return try {
                        val videoIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(Uri.parse(url), "video/*")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(Intent.createChooser(videoIntent, "Play video with..."))
                        true
                    } catch (e: Exception) {
                        false
                    }
                }

                if (url.startsWith("https://moxi-5n1.pages.dev") || 
                    url.startsWith("http://127.0.0.1") || 
                    url.startsWith("http://localhost")) {
                    return false
                }
                return try {
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                    true
                } catch (e: Exception) {
                    false
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Inject helper to automatically connect external player buttons to AndroidBridge
                val js = """
                    (function() {
                        if (window.__moxiInjected) return;
                        window.__moxiInjected = true;
                        document.addEventListener('click', function(e) {
                            var target = e.target;
                            var btn = target.closest('a, button, [role="button"]');
                            if (!btn) return;
                            var href = btn.getAttribute('href') || '';
                            if (href.startsWith('vlc://') || href.includes('/playlist') || href.endsWith('.m3u8')) {
                                e.preventDefault();
                                e.stopPropagation();
                                if (window.AndroidBridge && window.AndroidBridge.playInExternalPlayer) {
                                    window.AndroidBridge.playInExternalPlayer(href);
                                }
                            }
                        }, true);
                    })();
                """.trimIndent()
                view?.evaluateJavascript(js, null)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    Timber.tag("MoxiWeb").d("${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}")
                }
                return true
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    onHideCustomView()
                    return
                }
                customView = view
                customViewCallback = callback
                fullscreenContainer.addView(view)
                fullscreenContainer.visibility = View.VISIBLE
                webView.visibility = View.GONE

                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

            override fun onHideCustomView() {
                if (customView == null) return
                fullscreenContainer.removeView(customView)
                fullscreenContainer.visibility = View.GONE
                webView.visibility = View.VISIBLE
                customView = null
                customViewCallback?.onCustomViewHidden()

                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (customView != null) {
                    webView.webChromeClient?.onHideCustomView()
                } else if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
