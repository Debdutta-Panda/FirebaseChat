package com.algogence.firebasechat
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pixplicity.easyprefs.library.Prefs

class NotificationService : FirebaseMessagingService() {

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        Prefs.putString("fcm_token",p0)
        Prefs.putBoolean("fcm_synced",false)
        ChatBox.updateFcmToken()
    }


    override fun onMessageReceived(p0: RemoteMessage) {
        Log.d("new_message",p0.toString())
    }
}