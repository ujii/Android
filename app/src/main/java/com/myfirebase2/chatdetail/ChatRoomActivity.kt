package com.myfirebase2.chatdetail

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.myfirebase2.DBKey.Companion.DB_CHATS
import com.myfirebase2.R

class ChatRoomActivity : AppCompatActivity() {

    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }
    private var chatDB: DatabaseReference? = null

    private val chatList = mutableListOf<ChatItem>()
    private val adapter = ChatItemAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ChatRoomActivity", "intent: $intent")
        setContentView(R.layout.activity_chatroom)

        val chatKey = intent.getStringExtra("chatKey")
        Log.d("ChatRoomActivity", "chatKey from intent: $chatKey")

        chatDB = Firebase.database.reference.child(DB_CHATS).child("$chatKey")


        val chatRecyclerView = findViewById<RecyclerView>(R.id.chatRecyclerView)
        val sendButton = findViewById<Button>(R.id.sendButton)
        val messageEditText = findViewById<EditText>(R.id.messageEditText)


        chatRecyclerView.adapter = adapter
        chatRecyclerView.layoutManager = LinearLayoutManager(this)

        chatDB = Firebase.database.reference.child(DB_CHATS).child("$chatKey")
        Log.d("ChatKey1", "$chatKey")
        chatDB!!.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatItem = snapshot.getValue(ChatItem::class.java)
                chatItem ?: return
                Log.d("ChatRoomActivity", "${chatItem.message}, ${chatItem.senderId}")
                // 마지막 메시지로 스크롤
                chatRecyclerView.scrollToPosition(chatList.size - 1)
                chatList.add(chatItem)
                adapter.submitList(chatList)
                adapter.notifyDataSetChanged()
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onChildRemoved(snapshot: DataSnapshot) {}

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {}

        })

        sendButton.setOnClickListener {
            val message = messageEditText.text.toString().trim()

            if (message.isNotEmpty()) {
                val chatItem = ChatItem(
                    senderId = auth.currentUser?.uid ?: "",
                    message = message
                )

                chatDB!!.push().setValue(chatItem)

                // 메시지를 보낸 후 EditText 비우기
                messageEditText.text.clear()
            }
        }

    }
}
