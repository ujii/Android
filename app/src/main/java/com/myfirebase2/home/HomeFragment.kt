package com.myfirebase2.home

import com.myfirebase2.databinding.HomeFragmentBinding
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.myfirebase2.DBKey.Companion.CHILD_CHAT
import com.myfirebase2.DBKey.Companion.DB_ARTICLES
import com.myfirebase2.DBKey.Companion.DB_USER
import com.myfirebase2.R
import com.myfirebase2.chatlist.ChatListItem



class HomeFragment : Fragment(R.layout.home_fragment) {

    private lateinit var articleDB: DatabaseReference
    private lateinit var userDB: DatabaseReference
    private lateinit var articleAdapter: ArticleAdapter

    private val articleList = mutableListOf<ArticleModel>()
    private val listener = object: ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val articleModel = snapshot.getValue(ArticleModel::class.java)
            articleModel ?: return

            articleList.add(articleModel)
            articleAdapter.submitList(articleList)
            articleAdapter.notifyDataSetChanged()
        }


        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}

        override fun onChildRemoved(snapshot: DataSnapshot) {}

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

        override fun onCancelled(error: DatabaseError) {}
    }


    private var binding: HomeFragmentBinding? = null
    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        val fragmentHomeBinding = HomeFragmentBinding.bind(view)
        binding = fragmentHomeBinding

        articleDB = Firebase.database.reference.child(DB_ARTICLES)
        userDB = Firebase.database.reference.child(DB_USER)
        articleAdapter = ArticleAdapter(onItemClicked = { articleModel ->
            if (auth.currentUser != null) {

                val intent = Intent(context, ArticleDetail::class.java)
                intent.putExtra("articleTitle", articleModel.title)
                intent.putExtra("articleContent", articleModel.content)
                intent.putExtra("articlePrice", articleModel.price)
                intent.putExtra("articleSeller", articleModel.sellerId)
                intent.putExtra("articleSold", articleModel.sold)
                intent.putExtra("articleImage", articleModel.imageUrl)
                intent.putExtra("articleId", articleModel.articleId)
                intent.putExtra("uid", articleModel.uid)
                startActivity(intent)
                //Snackbar.make(view, "채팅방이 생성되었습니다. 채팅탭에서 확인해주세요.", Snackbar.LENGTH_LONG).show()

            } else {
                Snackbar.make(view, "로그인 후 사용해주세요.", Snackbar.LENGTH_LONG).show()
            }

        })
        articleList.clear()

        fragmentHomeBinding.articleRecyclerView.layoutManager = LinearLayoutManager(context)
        fragmentHomeBinding.articleRecyclerView.adapter = articleAdapter

        fragmentHomeBinding.addFloatingButton.setOnClickListener {
            context?.let {

                if (auth.currentUser != null) {
                    val intent = Intent(it, AddArticleActivity::class.java)
                    Log.v("auth.currentUser 값과 userDB 값", "${auth.currentUser} / ${userDB}")
                    startActivity(intent)
                } else {
                    Snackbar.make(view, "로그인 후 사용해주세요.", Snackbar.LENGTH_LONG).show()
                }

            }
        }


        articleDB.addChildEventListener(listener)

    }


    override fun onResume() {
        super.onResume()
        articleAdapter.notifyDataSetChanged()
    }


    override fun onDestroyView() {
        super.onDestroyView()

        articleDB.removeEventListener(listener)
    }

    // HomeFragment 내부에 추가
    fun filterItemsBySoldStatus(status: String) {
        val filteredList = articleList.filter { it.sold == status }
        articleAdapter.submitList(filteredList)
        articleAdapter.notifyDataSetChanged()
    }


}

