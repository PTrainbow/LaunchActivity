package com.ptrain.launchactivity

import android.app.Application
import android.content.Context

class MainApplication: Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        LaunchActivityUtils.isTargetActivity(MainActivity::class.java.simpleName)
    }
}