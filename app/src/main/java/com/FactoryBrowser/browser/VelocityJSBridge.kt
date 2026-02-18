package com.FactoryBrowser.browser

import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface
import android.webkit.WebView

class VelocityJSBridge(
    private val context: Context,
    private val webView: WebView,
    private val zoom: SmartZoomEngine
) {
    @JavascriptInterface fun applyZoom(kw: String)               { zoom.applyZoomForKeyword(kw) }
    @JavascriptInterface fun setZoomRule(kw: String, pct: Int)   { zoom.setRule(kw, pct) }
    @JavascriptInterface fun getZoom(): Int                       = zoom.getCurrentZoom()
    @JavascriptInterface fun getZoomRules(): String               = zoom.getRulesJson()
    @JavascriptInterface fun navigate(url: String)                { webView.post { webView.loadUrl(url) } }
    @JavascriptInterface fun goBack()                             { webView.post { if (webView.canGoBack()) webView.goBack() } }
    @JavascriptInterface fun reload()                             { webView.post { webView.reload() } }

    @JavascriptInterface fun triggerScan() {
        context.sendBroadcast(Intent("com.symbol.datawedge.api.ACTION").apply {
            putExtra("com.symbol.datawedge.api.SOFT_SCAN_TRIGGER", "START_SCANNING")
        })
    }

    @JavascriptInterface fun getDeviceInfo(): String {
        return """{"manufacturer":"${android.os.Build.MANUFACTURER}","model":"${android.os.Build.MODEL}","sdk":${android.os.Build.VERSION.SDK_INT}}"""
    }
}
