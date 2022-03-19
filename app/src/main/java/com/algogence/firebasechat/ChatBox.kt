package com.algogence.firebasechat

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.firebase.database.DataSnapshot
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject


class ChatBox(
    private val url: String,
    private val bucket: String,
    private val room: String,
    private val list: SnapshotStateList<Chat>
) {
    private val firepo = Firepo(url,"$bucket/$room")
    enum class ChatEvent{
        ADDED,
        REMOVED,
        CHANGED,
        MOVED,
        CANCELLED
    }
    private val DataSnapshot.toChat: Chat?
        get() {
            val value = value ?: ""
            val ms = Gson().toJson(value)
            val chat = try {
                Gson().fromJson(ms, Chat::class.java)
            } catch (e: Exception) {
                null
            }
            return chat
        }
    private fun onChildEvent(event: ChatEvent, chat: Chat?){
        when(event){
            ChatEvent.ADDED -> {
                if(chat!=null){
                    val index = list.indexOfFirst {
                        it.chatId==chat.chatId
                    }
                    if(index > -1){
                        list[index] = chat
                    }
                    else{
                        list.add(chat)
                        sortList()
                    }
                }
            }
            ChatEvent.REMOVED -> {
                if(chat!=null){
                    val index = list.indexOfFirst {
                        it.chatId == chat.chatId
                    }
                    if(index > -1){
                        list.removeAt(index)
                    }
                }
            }
            ChatEvent.CHANGED -> {
                if(chat!=null){
                    val index = list.indexOfFirst {
                        it.chatId == chat.chatId
                    }
                    if(index > -1){
                        list.set(index,chat)
                    }
                }
            }
            ChatEvent.MOVED -> {

            }
            ChatEvent.CANCELLED -> {

            }
        }
        childEventListener?.invoke(event,chat)
    }

    private fun sortList() {
        list.sortBy {
            it.createdAt
        }
    }

    var childEventListener: ((ChatEvent,Chat?)->Unit)? = null
    init {
        firepo.childEvent = {
            when(it.event){
                Firepo.ChildEventType.CHANGED->{
                    val chat = it.dataSnapshot?.toChat
                    onChildEvent(ChatEvent.CHANGED,chat)
                }
                Firepo.ChildEventType.REMOVED->{
                    val chat = it.dataSnapshot?.toChat
                    onChildEvent(ChatEvent.REMOVED,chat)
                }
                Firepo.ChildEventType.MOVED->{
                    val chat = it.dataSnapshot?.toChat
                    onChildEvent(ChatEvent.MOVED,chat)
                }
                Firepo.ChildEventType.ADDED -> {
                    val chat = it.dataSnapshot?.toChat
                    onChildEvent(ChatEvent.ADDED,chat)
                }
                Firepo.ChildEventType.CANCELLED -> {
                    onChildEvent(ChatEvent.CANCELLED,null)
                }
            }
        }
        startListening()
    }

    fun clear(){
        stopListening()
    }

    suspend fun chats(): List<Chat>{
        val snapshot = firepo.snapshot() ?: return emptyList()
        val value = snapshot.value
        val list = mutableListOf<Chat>()
        if(value is HashMap<*,*>){
            value.forEach {
                val s = it.value
                val js = Gson().toJson(s)
                val chat = Chat.fromString(js)
                if(chat!=null){
                    list.add(chat)
                }
            }
        }
        return list
    }

    fun insert(chat: Chat){
        firepo.insert(chat)
    }

    fun update(chat: Chat){
        firepo.update(
            "chatId",
            chat.chatId,
            chat
        )
    }

    private fun startListening(){
        CoroutineScope(Dispatchers.IO).launch {
            val s = System.currentTimeMillis()
            val chats = chats()
            val e = System.currentTimeMillis()
            val d = e - s
            Log.d("time_elapsed",d.toString())
            list.addAll(chats)
            sortList()
            firepo.startListening()
        }

    }

    private fun stopListening(){
        firepo.stopListening()
    }
}