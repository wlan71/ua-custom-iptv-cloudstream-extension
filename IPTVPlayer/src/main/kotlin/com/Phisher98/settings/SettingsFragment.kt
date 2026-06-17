package com.phisher98.settings

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lagradost.cloudstream3.plugins.Plugin

class SettingsFragment(val plugin: Plugin, val sharedPref: SharedPreferences) :
    BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(48, 48, 48, 48)
        }

        root.addView(TextView(ctx).apply {
            text = "Ukraine IPTV — Налаштування"
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 40)
        })

        root.addView(TextView(ctx).apply {
            text = "M3U Playlist URL"
            textSize = 13f
            setTextColor(Color.parseColor("#AAAAAA"))
            setPadding(0, 0, 0, 8)
        })

        val urlInput = EditText(ctx).apply {
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#666666"))
            hint = "https://your-playlist-url.m3u"
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            setPadding(24, 20, 24, 20)
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_URI
            setText(sharedPref.getString("playlist_url", ""))
        }
        root.addView(urlInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 8 })

        root.addView(TextView(ctx).apply {
            text = "Залиште порожнім для публічного UA IPTV списку"
            textSize = 12f
            setTextColor(Color.parseColor("#888888"))
            setPadding(0, 0, 0, 40)
        })

        val saveBtn = Button(ctx).apply {
            text = "ЗБЕРЕГТИ"
            setBackgroundColor(Color.parseColor("#0073E6"))
            setTextColor(Color.WHITE)
        }
        saveBtn.setOnClickListener {
            val url = urlInput.text.toString().trim()
            sharedPref.edit().putString("playlist_url", url).apply()
            Toast.makeText(ctx, "Збережено. Перезапустіть застосунок.", Toast.LENGTH_LONG).show()
            dismiss()
        }
        root.addView(saveBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 16 })

        val resetBtn = Button(ctx).apply {
            text = "СКИНУТИ ДО СТАНДАРТНОГО"
            setBackgroundColor(Color.parseColor("#333333"))
            setTextColor(Color.WHITE)
        }
        resetBtn.setOnClickListener {
            sharedPref.edit().remove("playlist_url").apply()
            urlInput.setText("")
            Toast.makeText(ctx, "Скинуто до UA IPTV.", Toast.LENGTH_SHORT).show()
            dismiss()
        }
        root.addView(resetBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        return root
    }
}
