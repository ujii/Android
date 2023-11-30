package com.myfirebase2.home

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.myfirebase2.Buying
import com.myfirebase2.DBKey.Companion.DB_ARTICLES
import com.myfirebase2.R
import com.myfirebase2.databinding.ActivityModifyArticleBinding

class ModifyArticleActivity : AppCompatActivity() {
    private var mBinding: ActivityModifyArticleBinding? = null
    private val binding get() = mBinding!!
    private var selectedUri: Uri? = null
    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }

    private val storage: FirebaseStorage by lazy {
        Firebase.storage
    }

    private val articleDB: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_ARTICLES)
    }

    private val getContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                handleImageSelection(data?.data)
            } else {
                Toast.makeText(this, "사진을 가져오지 못했습니다!!", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityModifyArticleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // 이전 정보 받아오기
        val oldTitle = intent.getStringExtra("oldArticleTitle")
        val oldContent = intent.getStringExtra("oldArticleContent")
        val oldPrice = intent.getStringExtra("oldArticlePrice")

        val oldIsSold = intent.getStringExtra("oldArticleSold")
        val oldImage = intent.getStringExtra("oldArticleImage")

        binding.titleEditText.setText(oldTitle)
        binding.priceEditText.setText(oldPrice)
        binding.ContentEditText.setText(oldContent)

        //binding.onSale.setText(oldIsSold)

        // Glide를 사용하여 이미지 로드 및 ImageView에 설정
        if (!oldImage.isNullOrEmpty()) {
            Glide.with(this)
                .load(oldImage)
                .into(binding.photoImageView)
        }


        binding.imageAddButton.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // 권한이 부여된 경우
                    getContent.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI))
                }
                shouldShowRequestPermissionRationale(android.Manifest.permission.READ_MEDIA_IMAGES) -> {
                    // 사용자가 권한 요청을 명시적으로 거부한 경우
                    showPermissionContextPopup()
                }
                else -> {
                    // 그 외
                    requestPermissions(arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES), 1010)
                }
            }
        }

        val radioGroup = findViewById<RadioGroup>(R.id.radioGroup)
        var sold: String = "판매중"

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            sold = when (checkedId) {
                R.id.reservation -> "예약중"
                R.id.soldOut -> "판매완료"
                R.id.sell -> "판매중"
                else-> "판매중"
            }
        }

        binding.submitButton.setOnClickListener {
            val title = binding.titleEditText.text.toString()
            val price = binding.priceEditText.text.toString()
            val content = binding.ContentEditText.text.toString()
            val sellerId = auth.currentUser?.email.orEmpty()

            val image = intent.getStringExtra("oldArticleImage")
            showProgress()

            // Get the selected radio button ID from the RadioGroup
            val selectedRadioButtonId = binding.radioGroup.checkedRadioButtonId


            val isSoldTextView = findViewById<TextView>(R.id.sold)


            if (selectedUri != null) {
                val photoUri = selectedUri ?: return@setOnClickListener
                uploadPhoto(
                    photoUri,
                    successHandler = { uri ->
                        // "아직 판매중"으로 초기화
                        updateArticle(sellerId, title, price, content, uri, sold)
                        val intent = Intent(this, Buying::class.java)
                        startActivity(intent)

                    },
                    errorHandler = {
                        Toast.makeText(this, "사진 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        hideProgress()
                    }
                )
            } else {
                // "아직 판매중"으로 초기화
                if (image != null) {
                    updateArticle(sellerId, title, price, content, image, sold)
                }
            }
        }
    }

    private fun uploadPhoto(uri: Uri, successHandler: (String) -> Unit, errorHandler: () -> Unit) {
        val fileName = "${System.currentTimeMillis()}.png"
        storage.reference.child("article/photo").child(fileName)
            .putFile(uri)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    storage.reference.child("article/photo").child(fileName).downloadUrl
                        .addOnSuccessListener { uri ->
                            successHandler(uri.toString())
                        }.addOnFailureListener {
                            errorHandler()
                        }
                } else {
                    errorHandler()
                }
            }
    }

    private fun updateArticle(
        sellerId: String,
        title: String,
        price: String,
        content: String,
        imageUrl: String,
        sold: String
    ) {
        val articleUpdates = hashMapOf<String, Any>(
            "title" to title,
            "price" to price,
            "content" to content,
            "imageUrl" to imageUrl,
            "sold" to sold
        )

        val articleId: String? = intent.getStringExtra("articleId")

        if (articleId != null) {
            articleDB.child(articleId).updateChildren(articleUpdates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        hideProgress()
                        Toast.makeText(this, "아이템이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                        // 수정이 완료되었을 때 Buying.kt 화면으로 이동
                        val intent = Intent(this, Buying::class.java)
                        startActivity(intent)
                        finish() // 현재 화면 종료(Optional: 현재 화면을 종료할 필요가 있는 경우)


                    } else {
                        hideProgress()
                        Toast.makeText(this, "아이템 수정에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "게시물 업데이트 중 오류 발생", task.exception)
                    }
                }
        } else {
            hideProgress()
            Toast.makeText(this, "잘못된 게시물 ID", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            1010 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 권한이 승인된 경우에 대한 처리
                    getContent.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "image/*"
                    })
                } else {
                    Toast.makeText(this, "권한을 거부하셨습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun showProgress() {
        findViewById<ProgressBar>(R.id.progressBar).isVisible = true
    }

    private fun hideProgress() {
        findViewById<ProgressBar>(R.id.progressBar).isVisible = false
    }

    private fun showPermissionContextPopup() {
        AlertDialog.Builder(this)
            .setTitle("권한이 필요합니다.")
            .setMessage("사진을 가져오기 위해 필요합니다.")
            .setPositiveButton("동의") { _, _ ->
                // 권한을 요청하는 코드를 추가
                requestPermissions(
                    arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES),
                    1010
                )
            }
            .setNegativeButton("취소") { _, _ ->
                Toast.makeText(this, "권한을 거부하셨습니다.", Toast.LENGTH_SHORT).show()
            }
            .create()
            .show()
    }

    private fun handleImageSelection(uri: Uri?) {
        if (uri != null) {
            binding.photoImageView.setImageURI(uri)
            selectedUri = uri
        } else {
            Toast.makeText(this, "사진을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}