package com.FactoryBrowser.browser

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.*

class LockdownActivity : Activity() {

    companion object {
        private const val REQ_STORAGE = 1001
        private const val ADMIN_PIN = "1234"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyKioskMode()
        requestStoragePermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        AppConfig.load(this)
        setContentView(buildUI())
    }

    private fun buildUI(): View {
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#0D0D1A"))
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        column.addView(TextView(this).apply {
            text = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}"
            textSize = 13f
            setTextColor(Color.parseColor("#444466"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, 40) }
        })

        column.addView(TextView(this).apply {
            text = "🏭  FactoryBrowser"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, 48) }
        })

        if (!AppConfig.isConfigLoaded) {
            column.addView(TextView(this).apply {
                text = "⚠️"
                textSize = 48f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 0, 0, 16) }
            })
            column.addView(TextView(this).apply {
                text = "Configuration file not found"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#FF6B6B"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 0, 0, 16) }
            })
            column.addView(TextView(this).apply {
                text = "Please place config file at:\n${AppConfig.getDeviceConfigFile().absolutePath}"
                textSize = 13f
                setTextColor(Color.parseColor("#888899"))
                gravity = Gravity.CENTER
                setPadding(48, 0, 48, 0)
            })
        } else {
            column.addView(TextView(this).apply {
                text = "OPEN BROWSER"
                textSize = 22f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#1565C0"))
                gravity = Gravity.CENTER
                setPadding(120, 40, 120, 40)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 0, 0, 20) }
                setOnClickListener {
                    startActivity(android.content.Intent(context, UrlPickerActivity::class.java))
                }
            })
            column.addView(TextView(this).apply {
                text = "Tap to select an application"
                textSize = 13f
                setTextColor(Color.parseColor("#555577"))
                gravity = Gravity.CENTER
            })
        }

        root.addView(column)

        root.addView(TextView(this).apply {
            text = "Admin"
            textSize = 12f
            setTextColor(Color.parseColor("#333355"))
            setPadding(24, 16, 24, 16)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.END
            ).also { it.setMargins(0, 0, 16, 16) }
            setOnClickListener { showAdminPinDialog() }
        })

        return root
    }

    private fun showAdminPinDialog() {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                        android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Enter PIN"
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(40, 20, 40, 20)
        }
        AlertDialog.Builder(this)
            .setTitle("Admin Access")
            .setMessage("Enter PIN to exit kiosk mode")
            .setView(input)
            .setPositiveButton("Confirm") { _, _ ->
                if (input.text.toString() == ADMIN_PIN) finishAffinity()
                else Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestStoragePermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQ_STORAGE)
                return
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQ_STORAGE) { AppConfig.load(this); setContentView(buildUI()) }
    }

    private fun applyKioskMode() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyKioskMode()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_APP_SWITCH -> true
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onBackPressed() {}
}
