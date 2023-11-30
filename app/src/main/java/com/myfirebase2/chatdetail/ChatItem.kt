package com.myfirebase2.chatdetail

data class ChatItem(
    val senderId: String,
    val message: String,
) {
    constructor(): this("", "")
}