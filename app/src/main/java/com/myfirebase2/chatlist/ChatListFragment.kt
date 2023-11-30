package com.myfirebase2.chatlist

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.myfirebase2.DBKey.Companion.CHILD_CHAT
import com.myfirebase2.DBKey.Companion.DB_USER
import com.myfirebase2.R
import com.myfirebase2.chatdetail.ChatRoomActivity
import com.myfirebase2.databinding.ChatlistFragmentBinding

class ChatListFragment : Fragment(R.layout.chatlist_fragment) {

    private lateinit var binding: ChatlistFragmentBinding
    private lateinit var chatListAdapter: ChatListAdapter
    private val chatRoomList = mutableListOf<ChatListItem>()

    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("ChatListFragment", "Fragment ID: ${System.identityHashCode(this)}")

        val fragmentChatlistBinding = ChatlistFragmentBinding.bind(view)

        binding = fragmentChatlistBinding

        initchartListAdapter()

        chatRoomList.clear()

        initChartRecyclerView()

        initChatDB()
    }


    private fun initchartListAdapter() {
        chatListAdapter = ChatListAdapter(onItemClicked = { chatRoom ->
            context?.let {
                val intent = Intent(it, ChatRoomActivity::class.java)
                intent.putExtra("chatKey", chatRoom.key) // 인텐트로 키를 전달해서 start
                Log.v("key", chatRoom.key.toString())
                startActivity(intent)
            }

        })
    }

    private fun initChartRecyclerView() {
        binding.chatRecyclerView.adapter = chatListAdapter
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun initChatDB() {
        val firebaseUser = auth.currentUser ?: return

        val chatDB =
            Firebase.database.reference.child(DB_USER).child(firebaseUser.uid).child(CHILD_CHAT)


        chatDB.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // 데이터 변경 시 호출되는 메서드
                chatRoomList.clear()
                snapshot.children.forEach {
                    val model = it.getValue(ChatListItem::class.java)
                    model ?: return
                    chatRoomList.add(model)
                    Log.d("ChatListFragment1", "ChatListItem added: $model")
                }
                chatListAdapter.submitList(chatRoomList)
                chatListAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatListFragment", "Database error: $error")
            }
        })

    }

    // view가 새로 그려졌을 때;
    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        Log.d("ChatListFragment", "onResume called")
    }

}