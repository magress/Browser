package com.FactoryBrowser.browser

import android.webkit.WebView

class SmartZoomEngine(private val webView: WebView) {

    // No hardcoded rules — all rules come from factory_config.json
    private val rules = mutableMapOf<String, Int>()
    private var currentZoom = 100

    fun setRule(keyword: String, zoom: Int) {
        rules[keyword.lowercase()] = zoom.coerceIn(25, 500)
    }

    fun getCurrentZoom() = currentZoom

    fun getRulesJson() = rules.entries.joinToString(",", "{", "}") {
        "\"${it.key}\":${it.value}"
    }

    fun detectAndApplyZoom() {
        if (rules.isEmpty()) return
        val js = """(function(){
          var t = (document.title||'').toLowerCase();
          var h = [].slice.call(document.querySelectorAll('h1,h2,h3,label,button'))
                    .map(function(e){return e.innerText||'';}).join(' ').toLowerCase();
          return t+' '+h;
        })();"""
        webView.evaluateJavascript(js) { result ->
            val text = result?.trim('"')?.lowercase() ?: ""
            applyZoom(resolve(text))
        }
    }

    fun applyZoomForKeyword(hint: String) = applyZoom(resolve(hint.lowercase()))

    private fun resolve(text: String): Int {
        var best: String? = null
        var zoom = 100
        rules.forEach { (kw, z) ->
            if (text.contains(kw) && (best == null || kw.length > best!!.length)) {
                best = kw
                zoom = z
            }
        }
        return zoom
    }

    private fun applyZoom(zoom: Int) {
        currentZoom = zoom
        webView.post { webView.setInitialScale(zoom) }
        val css = "html,body{margin:0 auto!important;display:flex!important;" +
                  "flex-direction:column!important;align-items:center!important;" +
                  "width:100%!important;}"
        val js = """(function(){
          var id='__fb';var e=document.getElementById(id);if(e)e.remove();
          var s=document.createElement('style');s.id=id;s.innerHTML='$css';
          document.head&&document.head.appendChild(s);
          window.scrollTo({top:0,behavior:'smooth'});
        })();"""
        webView.post { webView.evaluateJavascript(js, null) }
    }
}
