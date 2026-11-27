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

        /**
         * Called by injected JS when the HTML5 video element emits an error
         * (e.g. codec not supported, streaming server unreachable).
         * We immediately offer an external player so the user can still watch.
         */
        @android.webkit.JavascriptInterface
        fun onVideoPlaybackFailed(streamUrl: String, errorCode: Int) {
            Timber.w("Video playback failed in WebView – code=$errorCode url=$streamUrl")
            if (streamUrl.isBlank()) return
            runOnUiThread {
                try {
                    val cleanUrl = streamUrl.trim()
                    val videoIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(cleanUrl), "video/*")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(Intent.createChooser(videoIntent, "Stream not supported — open with…"))
                } catch (e: Exception) {
                    Timber.e(e, "External player fallback failed for: $streamUrl")
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
                // ── 1. External-player button bridge + video error fallback ──────────────
                val js = """
                    (function() {
                        if (window.__moxiInjected) return;
                        window.__moxiInjected = true;

                        // Intercept VLC / m3u8 href clicks
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

                        // Auto-detect <video> elements and fall back to external player on error
                        function attachVideoErrorHandler(video) {
                            if (video.__moxiErrorHandled) return;
                            video.__moxiErrorHandled = true;
                            video.addEventListener('error', function() {
                                var src = video.src || video.currentSrc || '';
                                var code = (video.error && video.error.code) ? video.error.code : 0;
                                if (src && src.length > 4 && window.AndroidBridge && window.AndroidBridge.onVideoPlaybackFailed) {
                                    window.AndroidBridge.onVideoPlaybackFailed(src, code);
                                }
                            });
                        }

                        // Watch DOM for video elements added dynamically
                        var videoObserver = new MutationObserver(function(mutations) {
                            mutations.forEach(function(mutation) {
                                mutation.addedNodes.forEach(function(node) {
                                    if (!node || node.nodeType !== 1) return;
                                    if (node.tagName === 'VIDEO') {
                                        attachVideoErrorHandler(node);
                                    } else {
                                        var vids = node.querySelectorAll ? node.querySelectorAll('video') : [];
                                        for (var i = 0; i < vids.length; i++) attachVideoErrorHandler(vids[i]);
                                    }
                                });
                            });
                        });
                        videoObserver.observe(document.documentElement, { childList: true, subtree: true });
                        // Also attach to any videos already present
                        document.querySelectorAll('video').forEach(attachVideoErrorHandler);
                    })();
                """.trimIndent()
                view?.evaluateJavascript(js, null)

                // ── 2. Pre-built addon installer (runs once, 4 s after page ready) ──────
                val addonJs = """
                    (function() {
                        if (window.__moxiAddonsScheduled) return;
                        window.__moxiAddonsScheduled = true;
                        setTimeout(function() {
                            try {
                                var PREBUILT = [
                                    { id: 'com.nuvio.tmdb.catalogs',  url: 'https://catalog.nuvio.tv/manifest.json' },
                                    { id: 'community.animestream',    url: 'https://animestream-addon.keypop3750.workers.dev/manifest.json' }
                                ];
                                // Reach stremio-core transport through the module registry it registers on window
                                var transport = window.__stremio_core_transport || (window.stremio && window.stremio.transport);
                                if (!transport || typeof transport.dispatch !== 'function') return;

                                // Ask the core for current addon list
                                transport.dispatch({ action: 'Ctx', args: { action: 'SyncLibraryWithAPI' } });

                                PREBUILT.forEach(function(addon) {
                                    transport.dispatch({
                                        action: 'Ctx',
                                        args: {
                                            action: 'InstallAddon',
                                            args: { transport_url: addon.url, flags: { protected: false } }
                                        }
                                    });
                                });
                            } catch(e) { /* silent – will retry on next load */ }
                        }, 4000);
                    })();
                """.trimIndent()
                view?.evaluateJavascript(addonJs, null)
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
