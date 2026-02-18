package com.FactoryBrowser.browser

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class UrlPickerActivity : Activity() {

    private val cardColors = listOf("#1565C0", "#6A1B9A", "#00695C", "#E65100")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppConfig.load(this)
        setContentView(buildUI())
    }

    override fun onResume() {
        super.onResume()
        AppConfig.load(this)
        setContentView(buildUI())
    }

    private fun buildUI(): View {
        val urls = AppConfig.urls

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D0D1A"))
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
        }

        // Header
        root.addView(TextView(this).apply {
            text = "🏭  FactoryBrowser"
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, 12) }
        })

        // Subtitle
        root.addView(TextView(this).apply {
            text = "Select an application to open"
            textSize = 14f
            setTextColor(Color.parseColor("#555577"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, 40) }
        })

        // Divider
        root.addView(View(this).apply {
            setBackgroundColor(Color.parseColor("#1E1E3A"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2
            ).also { it.setMargins(0, 0, 0, 40) }
        })

        // URL cards
        when {
            urls.isEmpty() -> {
                root.addView(TextView(this).apply {
                    text = "⚠️ No URLs configured\n\nPlace factory_config.json at:\n/sdcard/FactoryBrowser/factory_config.json"
                    textSize = 16f
                    setTextColor(Color.parseColor("#FF6B6B"))
                    gravity = Gravity.CENTER
                    setPadding(48, 48, 48, 48)
                })
            }
            urls.size <= 3 -> {
                root.addView(buildRow(urls.mapIndexed { i, u -> Pair(i, u) }))
            }
            else -> {
                root.addView(buildRow(urls.subList(0, 2).mapIndexed { i, u -> Pair(i, u) }))
                root.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 24)
                })
                root.addView(buildRow(urls.subList(2, 4).mapIndexed { i, u -> Pair(i + 2, u) }))
            }
        }

        return root
    }

    private fun buildRow(entries: List<Pair<Int, UrlEntry>>): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 280)
            entries.forEachIndexed { pos, (colorIdx, urlEntry) ->
                val card = buildCard(urlEntry, colorIdx)
                card.layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    .also { it.setMargins(if (pos > 0) 24 else 0, 0, 0, 0) }
                addView(card)
            }
        }
    }

    private fun buildCard(entry: UrlEntry, colorIndex: Int): FrameLayout {
        val accent = Color.parseColor(cardColors[colorIndex % cardColors.size])
        return FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#13132A"))
            elevation = 10f

            val inner = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setPadding(24, 24, 24, 24)
            }

            inner.addView(View(context).apply {
                setBackgroundColor(accent)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 8
                ).also { it.setMargins(0, 0, 0, 20) }
            })

            inner.addView(TextView(context).apply {
                text = entry.icon.ifEmpty { "🌐" }
                textSize = 36f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 0, 0, 12) }
            })

            inner.addView(TextView(context).apply {
                text = entry.label
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                maxLines = 2
            })

            addView(inner)
            setOnClickListener {
                startActivity(Intent(context, BrowserActivity::class.java).apply {
                    putExtra(BrowserActivity.EXTRA_URL,   entry.url)
                    putExtra(BrowserActivity.EXTRA_LABEL, entry.label)
                })
            }
            isClickable = true
            isFocusable = true
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { moveTaskToBack(true); return true }
        return super.onKeyDown(keyCode, event)
    }
}
