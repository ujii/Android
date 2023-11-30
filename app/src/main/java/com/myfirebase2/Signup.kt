
package com.myfirebase2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ktx.database

import com.google.firebase.ktx.Firebase


class Signup: AppCompatActivity() {

    private val TAG = "RegisterActivity"


    private lateinit var auth: FirebaseAuth

    val database = Firebase.database
    val myRef = database.getReference("Users")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val email = findViewById<EditText>(R.id.reg_email)
        val name = findViewById<EditText>(R.id.reg_name)
        val birth = findViewById<EditText>(R.id.reg_birth)
        val pwd = findViewById<EditText>(R.id.reg_pwd)
        val regBtn = findViewById<Button>(R.id.join_button)
        val backBtn = findViewById<Button>(R.id.go_back)


        regBtn.setOnClickListener {
            val email = email.text.toString().trim()
            val password = pwd.text.toString().trim()
            val name = name.text.toString().trim()
            val birth = birth.text.toString().trim()

            createUser(email, password, name, birth)
        }

        backBtn.setOnClickListener {

            backHome()
        }
    }



    private fun createUser(email: String, password: String, name: String, birth: String) {
        if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty() && birth.isNotEmpty()) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        updateUI(user, name, birth)
                        Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show()

                    } else {
                        Toast.makeText(this, "비밀번호를 6자리 이상 설정하세요", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "회원가입 실패", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateUI(user: FirebaseUser?, name: String, birth: String) {
        val userId = user?.uid
        val userRef = userId?.let { myRef.child(it) }
        if (userRef != null) {
            userRef.child("username").setValue(name)
        }
        if (userRef != null) {
            userRef.child("birth").setValue(birth)
        }
    }

    private fun backHome() {
        var intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}