package com.FactoryBrowser.browser

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

class BrowserActivity : Activity() {

    companion object {
        const val EXTRA_URL   = "extra_url"
        const val EXTRA_LABEL = "extra_label"
        const val DW_ACTION_RESULT = "com.FactoryBrowser.browser.SCAN_RESULT"
        const val DW_EXTRA_DATA    = "com.symbol.datawedge.data_string"
        const val DW_EXTRA_TYPE    = "com.symbol.datawedge.label_type"
        const val DW_API_ACTION    = "com.symbol.datawedge.api.ACTION"
        const val DW_SCANNER_CTRL  = "com.symbol.datawedge.api.SCANNER_INPUT_PLUGIN"
    }

    private lateinit var webView: WebView
    private lateinit var zoomEngine: SmartZoomEngine
    private lateinit var pageTitleView: TextView
    private lateinit var progressBar: ProgressBar

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != DW_ACTION_RESULT) return
            val barcode = intent.getStringExtra(DW_EXTRA_DATA) ?: return
            val type    = intent.getStringExtra(DW_EXTRA_TYPE) ?: "UNKNOWN"
            android.util.Log.d("Scanner", "Scanned: $barcode [$type]")
            injectScanIntoPage(barcode, type)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url   = intent.getStringExtra(EXTRA_URL)   ?: "https://example.com"
        val label = intent.getStringExtra(EXTRA_LABEL) ?: "Browser"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D0D1A"))
        }
        setContentView(root)

        // Top bar
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#13132A"))
            setPadding(8, 0, 16, 0)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 120)
        }

        topBar.addView(TextView(this).apply {
            text = "◀  Back"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#7986CB"))
            gravity = android.view.Gravity.CENTER
            setPadding(24, 0, 24, 0)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
            setOnClickListener { if (webView.canGoBack()) webView.goBack() else finish() }
        })

        topBar.addView(View(this).apply {
            setBackgroundColor(Color.parseColor("#2A2A4A"))
            layoutParams = LinearLayout.LayoutParams(2, LinearLayout.LayoutParams.MATCH_PARENT)
                .also { it.setMargins(0, 20, 16, 20) }
        })

        pageTitleView = TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(Color.parseColor("#AAAACC"))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        }
        topBar.addView(pageTitleView)

        topBar.addView(TextView(this).apply {
            text = "↻"
            textSize = 22f
            setTextColor(Color.parseColor("#555577"))
            gravity = android.view.Gravity.CENTER
            setPadding(24, 0, 8, 0)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
            setOnClickListener { webView.reload() }
        })

        root.addView(topBar)

        // Progress bar
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#7986CB"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 6)
            visibility = View.GONE
        }
        root.addView(progressBar)

        // WebView
        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        configureWebView()
        root.addView(webView)

        zoomEngine = SmartZoomEngine(webView)
        AppConfig.zoomRules.forEach { (k, v) -> zoomEngine.setRule(k, v) }

        webView.addJavascriptInterface(VelocityJSBridge(this, webView, zoomEngine), "VelocityNative")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                progressBar.visibility = View.VISIBLE
                progressBar.progress = 10
            }
            override fun onPageFinished(view: WebView, url: String) {
                progressBar.visibility = View.GONE
                pageTitleView.text = view.title?.takeIf { it.isNotEmpty() } ?: label
                injectBridgeJS()
                zoomEngine.detectAndApplyZoom()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress == 100) progressBar.visibility = View.GONE
            }
            override fun onReceivedTitle(view: WebView, title: String) {
                pageTitleView.text = title.takeIf { it.isNotEmpty() } ?: label
            }
        }

        webView.loadUrl(url)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(scanReceiver, IntentFilter(DW_ACTION_RESULT))
        sendBroadcast(Intent(DW_API_ACTION).putExtra(DW_SCANNER_CTRL, "ENABLE_PLUGIN"))
    }

    override fun onPause() {
        super.onPause()
        sendBroadcast(Intent(DW_API_ACTION).putExtra(DW_SCANNER_CTRL, "DISABLE_PLUGIN"))
        try { unregisterReceiver(scanReceiver) } catch (e: Exception) { }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) webView.goBack() else finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun injectScanIntoPage(barcode: String, type: String) {
        val safe = barcode.replace("\\", "\\\\").replace("'", "\\'")
            .replace("\n", "").replace("\r", "")

        val js = """
        (function() {
            var barcode = '$safe';
            var barcodeType = '$type';

            var focused = document.activeElement;
            if (focused && isInputField(focused)) { fillField(focused, barcode); return; }

            var inputs = document.querySelectorAll(
                'input[type="text"], input[type="search"], input[type="number"],' +
                'input[type="tel"], input:not([type]), textarea'
            );
            for (var i = 0; i < inputs.length; i++) {
                var input = inputs[i];
                if (isVisible(input) && !input.disabled && !input.readOnly) {
                    input.focus();
                    fillField(input, barcode);
                    return;
                }
            }

            if (window.VelocityBridge) window.VelocityBridge._onScan(barcode, barcodeType);

            function fillField(el, value) {
                var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value');
                if (setter) setter.set.call(el, value); else el.value = value;
                el.dispatchEvent(new Event('input',  { bubbles: true }));
                el.dispatchEvent(new Event('change', { bubbles: true }));
                el.dispatchEvent(new KeyboardEvent('keydown',  { key: 'Enter', keyCode: 13, bubbles: true }));
                el.dispatchEvent(new KeyboardEvent('keypress', { key: 'Enter', keyCode: 13, bubbles: true }));
                el.dispatchEvent(new KeyboardEvent('keyup',    { key: 'Enter', keyCode: 13, bubbles: true }));
            }

            function isInputField(el) {
                var tag = (el.tagName||'').toLowerCase();
                var type = (el.type||'').toLowerCase();
                if (tag === 'textarea') return true;
                if (tag === 'input') {
                    return ['button','submit','reset','checkbox','radio',
                            'file','hidden','image','range','color'].indexOf(type) === -1;
                }
                return false;
            }

            function isVisible(el) {
                var r = el.getBoundingClientRect();
                return r.width > 0 && r.height > 0 && r.top >= 0 && r.top <= window.innerHeight &&
                       window.getComputedStyle(el).visibility !== 'hidden' &&
                       window.getComputedStyle(el).display !== 'none';
            }
        })();
        """.trimIndent()

        webView.post { webView.evaluateJavascript(js, null) }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled        = true
            domStorageEnabled         = true
            databaseEnabled           = true
            loadWithOverviewMode      = true
            useWideViewPort           = true
            setSupportZoom(true)
            builtInZoomControls       = true
            displayZoomControls       = false
            cacheMode                 = WebSettings.LOAD_DEFAULT
            mixedContentMode          = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString           = "FactoryBrowser/1.0 (Android; ${android.os.Build.MANUFACTURER})"
        }
        WebView.setWebContentsDebuggingEnabled(true)
    }

    private fun injectBridgeJS() {
        val js = """
        (function() {
          if (window.__factoryBrowserInjected) return;
          window.__factoryBrowserInjected = true;
          window.VelocityBridge = {
            _listeners: [],
            _onScan: function(d,t){ this._listeners.forEach(function(fn){ fn(d,t); }); },
            onScan:      function(cb)    { this._listeners.push(cb); },
            applyZoom:   function(kw)    { VelocityNative.applyZoom(kw||document.title); },
            setZoomRule: function(kw,pct){ VelocityNative.setZoomRule(kw,pct); },
            getZoom:     function()      { return VelocityNative.getZoom(); },
            triggerScan: function()      { VelocityNative.triggerScan(); },
            navigate:    function(url)   { VelocityNative.navigate(url); }
          };
        })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }
}
