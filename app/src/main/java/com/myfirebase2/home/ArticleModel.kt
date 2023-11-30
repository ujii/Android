package com.myfirebase2.home

data class ArticleModel (
    val uid:String,
    val sellerId: String,
    val title: String,
    val createdAt: Long,
    val price: String,
    val content: String,
    val imageUrl: String,
    val sold: String,
    val articleId: String,
){
    constructor(): this("","","",0,"", "",  "", "판매중", "")
}