package com.algogence.firebasechat

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContextWrapper
import android.provider.Settings.Secure.*
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.pixplicity.easyprefs.library.Prefs

class App: Application() {
    @SuppressLint("HardwareIds")
    override fun onCreate() {
        super.onCreate()
        Prefs.Builder()
            .setContext(this)
            .setMode(ContextWrapper.MODE_PRIVATE)
            .setPrefsName(packageName)
            .setUseDefaultSharedPreference(true)
            .build()

        val id = getString(contentResolver,ANDROID_ID)
        Prefs.putString(ANDROID_ID,id)
        val fcm_synced = Prefs.getBoolean("fcm_synced")
        if(!fcm_synced){
            val fcm_token = Prefs.getString("fcm_token")
            if(fcm_token.isEmpty()){
                FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        return@OnCompleteListener
                    }
                    val token = task.result
                    Prefs.putString("fcm_token",token)
                    Prefs.putBoolean("fcm_synced",false)
                    ChatBox.updateFcmToken()
                })
            }
        }
    }
}