package com.algogence.firebasechat

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algogence.firebasechat.ui.theme.FirebaseChatTheme
import com.google.firebase.database.*
import com.pixplicity.easyprefs.library.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter


class MainActivity : ComponentActivity() {
    var chatBox: ChatBox? = null
    enum class Page{
        LOGIN,
        PEER,
        CHAT
    }
    private val messageCardCornerRadius = 6
    private val messageCardCornerElevation = 10
    private val messageCardMargin = 12
    private val messageCardPadding = 12
    private val messageCardMinSizeFactor = 0.25
    private val messageCardMaxSizeFactor = 0.75
    private val chats = mutableStateListOf<Chat>()
    val state = LazyListState()
    var myId = mutableStateOf(getUserId())
    val peerId = mutableStateOf("")
    val currentPage = mutableStateOf(Page.LOGIN)


    fun scrollToBottomForce(){
        CoroutineScope(Dispatchers.Main).launch {
            if(chats.isNotEmpty()){
                state.scrollToItem(chats.size-1)
            }
        }
    }
    private fun scrollToBottom() {
        if(chats.size>0){
            CoroutineScope(Dispatchers.Main).launch {
                val last = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index?:-1
                if(last==chats.size-2){
                    scrollToBottomForce()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPage()

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
        var popupMenu by remember {
            mutableStateOf(false)
        }
        if(message.sender!=myId.value&&message.seenAt==0L){
            chatBox?.markSeen(message)
        }
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
                        .align(if (message.sender == myId.value) Alignment.CenterEnd else Alignment.CenterStart)
                        .clickable {
                            popupMenu = true
                        },
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
                        .widthIn(
                            (configuration.screenWidthDp * messageCardMinSizeFactor).dp,
                            (configuration.screenWidthDp * messageCardMaxSizeFactor).dp
                        )
                        .width(IntrinsicSize.Min)
                    ){
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
                            Divider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = if(message.sender==myId.value) Color.LightGray else Color.Gray
                            )
                            Row(
                                modifier = Modifier.align(Alignment.End)
                            ){
                                val time = DateTime(message.createdAt,DateTimeZone.UTC).toDateTime(DateTimeZone.getDefault()).toString(
                                    DateTimeFormat.forPattern("hh:mm a")
                                )
                                Text(
                                    time,
                                    modifier = Modifier.wrapContentSize(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Visible,
                                    fontSize = 12.sp,
                                    color = if(message.sender==myId.value) Color.Gray else Color.White
                                )
                                if(message.sender==myId.value){
                                    when{
                                        message.seenAt>0->{
                                            Icon(
                                                imageVector = Icons.Filled.DoneAll,
                                                tint = Color(0xff035efc),
                                                contentDescription = "Seen",
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .padding(horizontal = 4.dp)
                                            )
                                        }
                                        message.receivedAt>0->{
                                            Icon(
                                                imageVector = Icons.Filled.DoneAll,
                                                tint = Color.Gray,
                                                contentDescription = "Received",
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .padding(horizontal = 4.dp)
                                            )
                                        }
                                        message.arrivedServerAt>0->{
                                            Icon(
                                                imageVector = Icons.Filled.Done,
                                                tint = Color.Gray,
                                                contentDescription = "Arrived at server",
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .padding(horizontal = 4.dp)
                                            )
                                        }
                                        else->{
                                            Icon(
                                                imageVector = Icons.Filled.AccessTime,
                                                tint = Color.Gray,
                                                contentDescription = "Waiting",
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .padding(horizontal = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            //Text(message.rtmMessage.serverReceivedTs.toString())
                        }
                    }
                }

                val context = LocalContext.current
                DropdownMenu(
                    expanded = popupMenu,
                    offset = DpOffset(0.dp, 0.dp),
                    onDismissRequest = { popupMenu = false }) {
                    listOf(
                        "Delete",
                        "Forward",
                        "Details"
                    ).forEach {
                        DropdownMenuItem(onClick = {
                            Toast.makeText(
                                context,
                                "You clicked $it menu",
                                Toast.LENGTH_LONG
                            ).show()
                            popupMenu = false
                        }) {
                            Text(text = it)
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
                chats,
                myId.value
            ){chat->
                if(chat==null){
                    scrollToBottomForce()
                }
                else{
                    if(chat.sender==myId.value){
                        scrollToBottomForce()
                    }
                    else{
                        scrollToBottom()
                    }
                }
            }
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
    }
}