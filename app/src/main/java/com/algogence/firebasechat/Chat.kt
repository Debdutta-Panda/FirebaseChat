package com.algogence.firebasechat

import com.google.gson.Gson

data class Chat(
    var chatId: String = "",
    var sender: String = "",
    var receiver: String = "",
    var data: ChatPacketData? = null,
    var createdAt: Long = 0,
    var arrivedServerAt: Long = 0,
    var receivedAt: Long = 0,
    var seenAt: Long = 0,
    var deleted: Boolean = false
)



data class ChatPackets(
    val items: List<Chat>
) {
    override fun toString(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun fromString(json: String): ChatPackets? {
            try {
                return Gson().fromJson(json, ChatPackets::class.java)
            } catch (e: Exception) {
            }
            return null
        }
    }
}


data class ChatPacketAttachment(
    val url: String? = null,
    val thumbnail: String? = null,
    val type: String,
    val name: String? = null,
    val json: String? = null
){
    fun clone(): ChatPacketAttachment{
        return ChatPacketAttachment(
            url,
            thumbnail,
            type,
            name,
            json
        )
    }
}


data class ChatPacketData(
    val text: String? = null,
    val attachments: List<ChatPacketAttachment>? = null
) {
    fun clone(): ChatPacketData{
        return ChatPacketData(
            text,
            attachments?.map {
                it.clone()
            }
        )
    }
    fun jsonString(): String {
        return Gson().toJson(this)
    }


    companion object {
        fun fromString(json: String): ChatPacketData? {
            try {
                return Gson().fromJson(json, ChatPacketData::class.java)
            } catch (e: Exception) {
            }
            return null
        }
    }
}