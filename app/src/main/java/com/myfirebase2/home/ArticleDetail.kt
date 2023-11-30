package com.myfirebase2.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.myfirebase2.Buying
import com.myfirebase2.DBKey
import com.myfirebase2.R
import com.myfirebase2.chatdetail.ChatRoomActivity
import com.myfirebase2.chatlist.ChatListItem
import com.myfirebase2.databinding.ItemArticleDetailBinding


class ArticleDetail : AppCompatActivity() {
    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }
    private lateinit var userDB: DatabaseReference

    // (전역변수) 바인딩 객체 선언
    private var vBinding : ItemArticleDetailBinding? = null

    // 매번 null 확인 귀찮음 -> 바인딩 변수 재선언
    private val binding get() = vBinding!!

    private lateinit var modifyArticleLauncher: ActivityResultLauncher<Intent>
    private lateinit var articleDB: DatabaseReference


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.item_article_detail)

        modifyArticleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                // 결과 데이터를 필요에 따라 처리합니다.
                if (data != null) {
                    val modifiedTitle = data.getStringExtra("modifiedTitle")
                    val modifiedContent = data.getStringExtra("modifiedContent")
                    val modifiedPrice = data.getStringExtra("modifiedPrice")
                    val modifiedSold = data.getStringExtra("modifiedSold")
                    // ... 다른 수정된 데이터 처리
                    updateUI(modifiedTitle, modifiedContent, modifiedPrice, modifiedSold, /* ... */)
                }
            }
        }


        val toolbar: Toolbar = findViewById(R.id.detailToolbar)
        setSupportActionBar(toolbar)

        val sellerID = findViewById<TextView>(R.id.sellerID)
        val sold = findViewById<TextView>(R.id.sold)
        val image = findViewById<ImageView>(R.id.photoImageView)
        val title = findViewById<TextView>(R.id.titleTextView)
        val content = findViewById<TextView>(R.id.ContentTextView)
        val price = findViewById<TextView>(R.id.priceTextView)

        sellerID.text = intent.getStringExtra("articleSeller")
        sold.text = intent.getStringExtra("articleSold")
        title.text = intent.getStringExtra("articleTitle")
        content.text = intent.getStringExtra("articleContent")
        price.text = intent.getStringExtra("articlePrice")
        val articleId = intent.getStringExtra("articleId")
        Log.d("ArticleDetail", "Selected articleId: $articleId")


        // 이미지 URL 가져오기
        val imageUri = intent.getStringExtra("articleImage")

        // Glide를 사용하여 이미지 로드
        Glide.with(this)
            .load(imageUri)
            .into(image)

        val chatBtn = findViewById<Button>(R.id.chatButton)
        userDB = Firebase.database.reference.child(DBKey.DB_USER)


        chatBtn.setOnClickListener {
            if (auth.currentUser != null) {
                val chatRoom = ChatListItem(
                    sellerUID = intent.getStringExtra("uid") ?: "",
                    buyerId = auth.currentUser?.email ?: "",
                    sellerId = intent.getStringExtra("articleSeller") ?: "",
                    itemTitle = intent.getStringExtra("articleTitle") ?: "",
                    key = System.currentTimeMillis().toString()
                )
                Log.d("Article Detail's chatRoom", "$chatRoom")

                if (chatRoom.buyerId == chatRoom.sellerId) {
                    Toast.makeText(this, "자신이 작성한 게시글입니다.", Toast.LENGTH_SHORT).show()
                }else {
                    userDB.child(auth.currentUser!!.uid)
                        .child(DBKey.CHILD_CHAT)
                        .push()
                        .setValue(chatRoom)

                    userDB.child(chatRoom.sellerUID)
                        .child(DBKey.CHILD_CHAT)
                        .push()
                        .setValue(chatRoom)


                    val newIntent = Intent(this, ChatRoomActivity::class.java)
                    newIntent.putExtra("chatKey", chatRoom.key)
                    startActivity(newIntent)
                }
            }
        }

    }




    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.modify_bar, menu)
        val articleAuthorId = intent.getStringExtra("articleSeller") // Get the ID of the article author
        val currentUser = FirebaseAuth.getInstance().currentUser // Get the currently logged-in user

        menu?.findItem(R.id.modify)?.isVisible = currentUser != null && currentUser?.email == articleAuthorId
        menu?.findItem(R.id.delete)?.isVisible = currentUser != null && currentUser?.email == articleAuthorId
        return true
    }

    // ArticleDetail 클래스 상단에 아래와 같이 상수 정의
    companion object {
        const val MODIFY_ARTICLE_REQUEST = 1001
    }

    // ArticleDetail 클래스에서 onActivityResult 메서드 추가
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == MODIFY_ARTICLE_REQUEST && resultCode == Activity.RESULT_OK) {
            val modifiedTitle = data?.getStringExtra("modifiedTitle")
            val modifiedContent = data?.getStringExtra("modifiedContent")
            val modifiedPrice = data?.getStringExtra("modifiedPrice")
            val modifiedSold = data?.getStringExtra("modifiedSold")

            // 데이터를 받은 후에 UI 업데이트 메서드 호출
            updateUI(modifiedTitle, modifiedContent, modifiedPrice, modifiedSold, /* ... */)
        }
    }


    // ArticleDetail 클래스에서 ModifyArticleActivity 호출 코드 수정
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.modify -> {
                // 수정하기 메뉴를 눌렀을 때 동작하는 코드
                // activity_add_article 로 이동
                // ArticleDetail 액티비티에서 ModifyArticleActivity로 이동할 때

                // 이미지 URL 가져오기
                val imageUri = intent.getStringExtra("articleImage")
                val articleId = intent.getStringExtra("articleId")

                val intent = Intent(this, ModifyArticleActivity::class.java).apply {
                    val sellerID = findViewById<TextView>(R.id.sellerID).text.toString()
                    val sold = findViewById<TextView>(R.id.sold).text.toString()
                    val title = findViewById<TextView>(R.id.titleTextView).text.toString()
                    val content = findViewById<TextView>(R.id.ContentTextView).text.toString()
                    val price = findViewById<TextView>(R.id.priceTextView).text.toString()

                    // 이전 정보를 ModifyArticleActivity로 넘겨줌
                    putExtra("oldArticleTitle", title)
                    putExtra("oldArticleContent", content)
                    putExtra("oldArticlePrice", price)
                    putExtra("oldArticleSeller", sellerID)
                    putExtra("oldArticleSold", sold)
                    putExtra("oldArticleImage", imageUri)
                    putExtra("articleId", articleId) // 이 부분을 추가
                }

                // 아래 라인을 수정합니다.
                modifyArticleLauncher.launch(intent)

                true
            }
            R.id.delete ->{

                val articleId = intent.getStringExtra("articleId")
                articleDB = Firebase.database.reference.child(DBKey.DB_ARTICLES)
                if (articleId != null) {
                    val articleRef = articleDB.child(articleId)
                    articleRef.removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(this, "게시물이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, Buying::class.java)
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "게시물 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "게시물 ID를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    // ArticleDetail 클래스에 수정된 데이터를 화면에 갱신하는 메서드 추가
    private fun updateUI(modifiedTitle: String?, modifiedContent: String?, modifiedPrice: String?, modifiedSold: String?) {
        Log.d("ArticleDetail", "updateUI called with: title=$modifiedTitle, content=$modifiedContent, price=$modifiedPrice")
        // 수정된 데이터를 화면에 갱신
        val titleTextView = findViewById<TextView>(R.id.titleTextView)
        val contentTextView = findViewById<TextView>(R.id.ContentTextView)
        val priceTextView = findViewById<TextView>(R.id.priceTextView)
        val soldTextView = findViewById<TextView>(R.id.sold)
        // ... 다른 UI 업데이트

        modifiedTitle?.let { titleTextView.text = it }
        modifiedContent?.let { contentTextView.text = it }
        modifiedPrice?.let { priceTextView.text = it }
        modifiedSold?.let { soldTextView.text = it }
        // ... 다른 수정된 데이터에 대한 UI 업데이트
    }


}