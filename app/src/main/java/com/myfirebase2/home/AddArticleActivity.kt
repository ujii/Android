package com.myfirebase2.home

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.myfirebase2.DBKey.Companion.DB_ARTICLES
import com.myfirebase2.R
import com.myfirebase2.databinding.ActivityAddArticleBinding

class AddArticleActivity : AppCompatActivity() {
    private var mBinding: ActivityAddArticleBinding? = null
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
        mBinding = ActivityAddArticleBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        binding.submitButton.setOnClickListener {
            val uid = auth.currentUser!!.uid
            val title = binding.titleEditText.text.toString()
            val price = binding.priceEditText.text.toString()
            val content = binding.ContentEditText.text.toString()
            val sellerId = auth.currentUser?.email.orEmpty()
            val articleId = auth.currentUser?.providerId
            val sold:String = "판매중"

            showProgress()

            if (selectedUri != null) {
                val photoUri = selectedUri ?: return@setOnClickListener
                uploadPhoto(
                    photoUri,
                    successHandler = { uri ->
                        // "판매중"으로 초기화
                        if (articleId != null) {
                            uploadArticle(uid, sellerId, title, price, content, uri, sold, articleId)
                        }
                    },
                    errorHandler = {
                        Toast.makeText(this, "사진 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        hideProgress()
                    }
                )
            } else {
                // ""판매중"으로 초기화
                if (articleId != null) {
                    uploadArticle(uid, sellerId, title, price, content, "", "판매중", articleId)
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

    private fun uploadArticle(uid:String, sellerId: String, title: String, price: String, content: String, imageUrl: String, sold: String, articleId: String) {
        // 아이템을 추가할 때 push를 사용하여 고유한 키를 생성하고 해당 키를 반환합니다.
        val newArticleRef = articleDB.push()

        // 반환된 키를 articleId로 사용합니다.
        val articleId = newArticleRef.key.orEmpty()

        // 생성된 articleId를 사용하여 아이템 추가
        val model = ArticleModel(auth.currentUser!!.uid, sellerId, title, System.currentTimeMillis(), price, content, imageUrl, sold, articleId)

        newArticleRef.setValue(model)
            .addOnSuccessListener {
                hideProgress()
                Toast.makeText(this, "아이템이 등록되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                hideProgress()
                Toast.makeText(this, "아이템 등록에 실패했습니다.", Toast.LENGTH_SHORT).show()
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