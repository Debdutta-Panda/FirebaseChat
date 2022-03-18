package com.algogence.firebasechat

import com.google.firebase.database.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class Firepo(private val url: String, private val path: String) {
    private val databaseReference = FirebaseDatabase.getInstance(url).reference

    suspend fun snapshot(): DataSnapshot? =
        suspendCoroutine { cont ->
            getSingleValue{success,snapshot,error->
                if(success){
                    cont.resume(snapshot)
                }
                else{
                    cont.resume(null)
                }
            }
        }

    fun getSingleValue(callback: (Boolean,DataSnapshot?,DatabaseError?)->Unit){
        databaseReference.child(path).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                callback(true,dataSnapshot,null)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                callback(false,null,databaseError)
            }
        })
    }

    enum class ChildEventType{
        ADDED,
        CHANGED,
        REMOVED,
        MOVED,
        CANCELLED
    }

    data class ChildEvent(
        val event: ChildEventType,
        val dataSnapshot: DataSnapshot? = null,
        val previousChildName: String? = null,
        val error: DatabaseError? = null
    )

    var childEvent: ((ChildEvent)->Unit)? = null

    private var childEventListener = object: ChildEventListener{
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            childEvent?.invoke(
                ChildEvent(
                    ChildEventType.ADDED,
                    snapshot,
                    previousChildName,
                    null
                )
            )
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            childEvent?.invoke(
                ChildEvent(
                    ChildEventType.CHANGED,
                    snapshot,
                    previousChildName,
                    null
                )
            )
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
            childEvent?.invoke(
                ChildEvent(
                    ChildEventType.REMOVED,
                    snapshot,
                    null,
                    null
                )
            )
        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            childEvent?.invoke(
                ChildEvent(
                    ChildEventType.MOVED,
                    snapshot,
                    previousChildName,
                    null
                )
            )
        }

        override fun onCancelled(error: DatabaseError) {
            childEvent?.invoke(
                ChildEvent(
                    ChildEventType.CANCELLED,
                    null,
                    null,
                    error
                )
            )
        }
    }

    var isListening = false
    fun registerChildEvents(){
        if(isListening){
            return
        }
        databaseReference.child(path).addChildEventListener(childEventListener)
        isListening = true
    }

    fun unregisterChildEvents(){
        if(!isListening){
            return
        }
        databaseReference.child(path).removeEventListener(childEventListener)
        isListening = false
    }

    fun startListening(){
        registerChildEvents()
    }

    fun stopListening(){
        unregisterChildEvents()
    }

    fun insert(value: Any?){
        databaseReference
            .child(path)
            .push()
            .setValue(
                value
            )
    }

    fun update(key: String, value: String, itemValue: Any){
        databaseReference
            .child(path)
            .orderByChild(key)
            .equalTo(value)
            .get()
            .addOnSuccessListener {
                val k = it.key
                databaseReference
                    .child("$path/$k")
                    .push()
                    .setValue(
                        value
                    )
            }
    }
}