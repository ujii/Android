package com.myfirebase2.chatlist

data class ChatListItem(
    val sellerUID: String,
    val buyerId: String,
    val sellerId: String,
    val key: String,
    val itemTitle: String,
) {
    constructor(): this("","", "", "", "")
}