package com.phisher98

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.phisher98.settings.SettingsFragment

@CloudstreamPlugin
class IPTVPlayerPlugin : Plugin() {
    override fun load(context: Context) {
        val sharedPref = context.getSharedPreferences("IPTVPlayer", Context.MODE_PRIVATE)
        registerMainAPI(IPTVPlayer(sharedPref))
        val activity = context as AppCompatActivity
        openSettings = {
            val frag = SettingsFragment(this, sharedPref)
            frag.show(activity.supportFragmentManager, "SettingsFragment")
        }
    }
}
