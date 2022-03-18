package com.algogence.firebasechat

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
    private val room: String
) {
    private var list: SnapshotStateList<Chat>? = null
    private val firepo = Firepo(url,"$bucket/$room")
    enum class ChatEvent{
        ADDED,
        REMOVED,
        CHANGED,
        MOVED,
        CANCELLED
    }

    fun setConsumer(list: SnapshotStateList<Chat>){
        this.list = list
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
    fun onChildEvent(event: ChatEvent,chat: Chat?){
        when(event){
            ChatEvent.ADDED -> {
                if(chat!=null){
                    list?.add(chat)
                }
            }
            ChatEvent.REMOVED -> {
                if(chat!=null){
                    val index = list?.indexOfFirst {
                        it.chatId == chat.chatId
                    }?:-1
                    if(index > -1){
                        list?.removeAt(index)
                    }
                }
            }
            ChatEvent.CHANGED -> {
                if(chat!=null){
                    val index = list?.indexOfFirst {
                        it.chatId == chat.chatId
                    }?:-1
                    if(index > -1){
                        list?.set(index,chat)
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
        val j = try {
            JSONObject(value.toString())
        } catch (e: Exception) {
            null
        } ?: return emptyList()
        val list = mutableListOf<Chat>()
        j.keys().forEach {
            val v = j.getString(it)
            val chat: Chat? = try {
                Gson().fromJson(v,Chat::class.java)
            } catch (e: Exception) {
                null
            }
            if(chat!=null){
                list.add(chat)
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

    fun startListening(){
        CoroutineScope(Dispatchers.IO).launch {
            val chats = chats()
            list?.addAll(chats)
            firepo.startListening()
        }

    }

    fun stopListening(){
        firepo.stopListening()
    }
}