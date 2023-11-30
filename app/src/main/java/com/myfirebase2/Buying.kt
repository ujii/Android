package com.myfirebase2

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.myfirebase2.chatlist.ChatListFragment

import com.myfirebase2.home.HomeFragment

class Buying: AppCompatActivity() {
    val homeFragment = HomeFragment()

    lateinit var storage: FirebaseStorage
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buying)

        val chatListFragment = ChatListFragment()
        var selectedUri: Uri? = null

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        replaceFragment(homeFragment)

        setSupportActionBar(findViewById(R.id.homeToolbar)) // R.id.toolbar는 여러분이 사용하는 툴바의 ID입니다.

        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> replaceFragment(homeFragment)
                R.id.chatList -> replaceFragment(chatListFragment)
            }
            true
        }
        Firebase.auth.currentUser ?: finish() // if not authenticated, finish this activity
        storage = Firebase.storage
        //displayImageRef()


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.option_bar, menu)
        Log.d("BuyingActivity", "onCreateOptionsMenu 호출됨")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.reserv -> {
                homeFragment.filterItemsBySoldStatus("예약중")
                return true
            }
            R.id.sell -> {
                // "판매 중"인 아이템만 보여주도록 HomeFragment에 명령
                homeFragment.filterItemsBySoldStatus("판매중")
                return true
            }
            R.id.sold_out -> {
                // "예약 중"인 아이템만 보여주도록 HomeFragment에 명령
                homeFragment.filterItemsBySoldStatus("판매완료")
                return true
            }
        }
        return true
    }



    private fun replaceFragment(fragment: Fragment){
        supportFragmentManager.beginTransaction()
            .apply {
                replace(R.id.fragment_container, fragment)
                commit()
            }
    }


}