package com.algogence.firebasechat

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algogence.firebasechat.ui.theme.FirebaseChatTheme
import com.google.firebase.database.*
import com.pixplicity.easyprefs.library.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    var chatBox: ChatBox? = null
    enum class Page{
        LOGIN,
        PEER,
        CHAT
    }


    /*private val valueEventListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            val numChildren = dataSnapshot.childrenCount
            val value = dataSnapshot.value.toString()
            Log.d("value_event_single",numChildren.toString())
            Log.d("value_event_single",value)
        }

        override fun onCancelled(databaseError: DatabaseError) {}
    }*/
    //private var isListening = false
    /*private val childEventListener = object: ChildEventListener{
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            Log.d("child_event_add",snapshot.key.toString())
            val value = snapshot.value
            Log.d("child_event_add_value",value.toString())
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            Log.d("child_event_change",snapshot.key.toString())
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
            Log.d("child_event_remove","removed")
        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            Log.d("child_event_move",previousChildName.toString())
        }

        override fun onCancelled(error: DatabaseError) {
            Log.d("child_event_erro","error")
        }

    }
    private val DATABASE_URL = "https://fir-chat-ad096-default-rtdb.asia-southeast1.firebasedatabase.app"
    private val database = FirebaseDatabase.getInstance(DATABASE_URL).reference*/

    //////////////////////////////////////
    private val messageCardCornerRadius = 6
    private val messageCardCornerElevation = 10
    private val messageCardMargin = 12
    private val messageCardPadding = 12
    private val messageCardMinSizeFactor = 0.125
    private val messageCardMaxSizeFactor = 0.75
    private val chats = mutableStateListOf<Chat>()
    val state = LazyListState()
    var myId = mutableStateOf(getUserId())
    val peerId = mutableStateOf("")
    val currentPage = mutableStateOf(Page.LOGIN)


    private fun scrollToBottom() {
        if(chats.size>0){
            CoroutineScope(Dispatchers.Main).launch {
                if(chats.isNotEmpty()){
                    state.scrollToItem(chats.size-1)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPage()
        /*lifecycleScope.launch {
            val resp = App.instnace.charc.testRest()
            Log.d("test_rest",resp.data?.message?:"no response")
        }*/

        /*App.instnace.queryFcmToken {
            Log.d("fcm_token_query",it?:"not found")
        }*/

        /*App.instnace.charc.sendChat()*/

        if(savedInstanceState==null){
            registerOnChange()
            myId.value = getUserId()
        }


        setContent {
            FirebaseChatTheme() {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    when(currentPage.value){
                        Page.LOGIN -> LoginPage()
                        Page.PEER -> PeerPage()
                        Page.CHAT -> ChatPage()
                    }
                }
            }
        }
    }

    /*private fun listenRoom() {
        val path = getReferencePath()
        database.child((path)).addListenerForSingleValueEvent(valueEventListener)
        database.child(path).addChildEventListener(childEventListener)


        isListening = true
    }*/


    private fun registerOnChange() {
        //*
    }

    @Composable
    private fun ChatPage() {
        val toSend = remember { mutableStateOf("") }
        Box(modifier = Modifier.fillMaxSize()){
            Column(modifier = Modifier.fillMaxSize()){
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color.Blue)){
                    Text(
                        peerId.value,
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                    IconButton(onClick = {
                        deleteAllChats(peerId.value)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            tint = Color.White,
                            contentDescription = ""
                        )
                    }
                }
                LazyColumn(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xfff5f5f5))
                        .weight(1f),
                    state
                ){
                    items(chats){
                        MessageItem(it)
                    }

                }
                Row(modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
                    .height(64.dp)){
                    OutlinedTextField(
                        value = toSend.value,
                        onValueChange = {
                            toSend.value = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    IconButton(onClick = {
                        val text = toSend.value
                        if(text.isEmpty()){
                            toast("Message needed")
                            return@IconButton
                        }
                        toSend.value = ""
                        sendMessage(text)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Send",
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }
        }
    }
    @Composable
    private fun LazyItemScope.MessageItem(message: Chat) {
        val chapPacketData = message.data
        val configuration = LocalConfiguration.current
        Box(modifier = Modifier.fillMaxWidth()){
            Box(modifier = Modifier
                .width((configuration.screenWidthDp * messageCardMaxSizeFactor).dp)
                .padding(messageCardMargin.dp)
                .align(
                    if (message.sender == myId.value) Alignment.CenterEnd else Alignment.CenterStart
                )
            ){
                Card(
                    modifier = Modifier
                        .wrapContentSize()
                        .align(if (message.sender == myId.value) Alignment.CenterEnd else Alignment.CenterStart),
                    elevation = messageCardCornerElevation.dp,
                    backgroundColor = if(message.sender == myId.value) Color.White else Color(0xff3838ff),
                    shape = RoundedCornerShape(
                        topStart = messageCardCornerRadius.dp,
                        topEnd = messageCardCornerRadius.dp,
                        bottomEnd = if(message.sender == myId.value) messageCardCornerRadius.dp else 0.dp,
                        bottomStart = if(message.sender == myId.value) 0.dp else messageCardCornerRadius.dp
                    ),
                ) {
                    Box(modifier = Modifier
                        .widthIn((configuration.screenWidthDp*messageCardMinSizeFactor).dp,(configuration.screenWidthDp*messageCardMaxSizeFactor).dp)){
                        Column(modifier = Modifier
                            .align(if (message.sender == myId.value) Alignment.CenterEnd else Alignment.CenterStart)
                            .wrapContentSize()
                            .padding(messageCardPadding.dp),
                            horizontalAlignment = if(message.sender == myId.value) Alignment.End else Alignment.Start
                        ){
                            if(message.sender != myId.value){
                                Text(
                                    message.sender,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                /*Divider(
                                    color = Color(0xff5757ff),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )*/
                            }
                            Text(
                                chapPacketData?.text?:"",
                                color = if(message.sender == myId.value) Color.Blue else Color.White
                            )
                            //Text(message.rtmMessage.serverReceivedTs.toString())
                        }
                    }
                }
            }
        }
    }

    private fun toast(s: String) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    }

    private fun sendMessage(text: String) {
        chatBox?.insert(createNewTextChat(text))
        /*database
            .child(getReferencePath())
            .push()
            .setValue(
                createNewTextChat(text)
            )*/
    }

    private fun getReferencePath(): String {
        return "messages/${getRoom()}"
    }

    private fun getRoom(): String {
        val i = myId.value
        val u = peerId.value
        return if(i<u){
            "${i}_${u}"
        } else{
            "${u}_${i}"
        }
    }

    private fun createNewTextChat(text: String): Chat {
        return Chat(
            data = ChatPacketData(text = text),
            sender = myId.value,
            receiver = peerId.value,
            chatId = newUid,
            createdAt = utcTimestamp
        )
    }

    private fun deleteAllChats(peerId: String) {

    }

    @Composable
    fun PeerPage() {
        val peerId = remember { mutableStateOf("") }
        Box(
            modifier = Modifier.fillMaxSize()
        ){
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(24.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Peer ID",
                    fontSize = 30.sp,
                    color = Color.Blue,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.size(24.dp))
                OutlinedTextField(
                    value = peerId.value,
                    onValueChange = {
                        peerId.value = it
                    },
                    label = {
                        Text("Peer ID")
                    },
                    placeholder = {
                        Text("Peer ID")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.size(24.dp))
                Button(
                    onClick = {
                        onStartClick(peerId.value)
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Blue
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Start",
                        color = Color.White,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    }

    private fun onStartClick(value: String) {
        if(value.isEmpty()){
            Toast.makeText(this, "Please put peer id", Toast.LENGTH_SHORT).show()
        }
        else{
            peerId.value = value
            chatBox = ChatBox(
                "https://fir-chat-ad096-default-rtdb.asia-southeast1.firebasedatabase.app",
                "messages",
                getRoom(),
                chats
            )
            //listenRoom()
            setPage()
            populate()
        }
    }

    private fun populate() {
        //*
    }

    @Composable
    fun LoginPage() {
        val senderId = remember { mutableStateOf("") }
        Box(
            modifier = Modifier.fillMaxSize()
        ){
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(24.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Login",
                    fontSize = 30.sp,
                    color = Color.Blue,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.size(24.dp))
                OutlinedTextField(
                    value = senderId.value,
                    onValueChange = {
                        senderId.value = it
                    },
                    label = {
                        Text("User ID")
                    },
                    placeholder = {
                        Text("User ID")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.size(24.dp))
                Button(
                    onClick = {
                        onLoginClick(senderId.value)
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Blue
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Login",
                        color = Color.White,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    }

    private fun onLoginClick(senderId: String) {
        if(senderId.isEmpty()){
            Toast.makeText(this, "Please put user id", Toast.LENGTH_SHORT).show()
        }
        else{
            saveSenderId(senderId)
            myId.value = senderId
            setPage()
        }
    }

    private fun saveSenderId(senderId: String) {
        Prefs.putString("userid",senderId)
        myId.value = senderId
    }

    private fun getUserId(): String {
        return Prefs.getString("userid")
    }

    private fun setPage() {
        currentPage.value = when {
            getUserId().isEmpty() -> {
                Page.LOGIN
            }
            peerId.value.isEmpty() -> {
                Page.PEER
            }
            else -> {
                Page.CHAT
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        chatBox?.clear()
        /*if(isListening){
            val path = getReferencePath()
            database.child(path).removeEventListener(childEventListener)
        }*/
    }
}