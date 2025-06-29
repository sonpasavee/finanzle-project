package com.example.projectfinanzle

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.projectfinanzle.LoginActivity
import com.example.projectfinanzle.R
import com.example.projectfinanzle.databinding.ActivityRegisterBinding
import com.google.common.base.Objects
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database :FirebaseDatabase
    private lateinit var databaseRef :DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Initialize FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance()


        binding.btnCreateAccount.setOnClickListener {
            val email = binding.edtEmail.text.toString()
            val password = binding.edtPassword.text.toString()
            val confirmPass = binding.edtConfirmPassword.text.toString()
            val username = binding.edtUsername.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && confirmPass.isNotEmpty() && username.isNotEmpty()) {
                if (password == confirmPass) {
                    firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                        if (it.isSuccessful) {
                            //create user in realtime database
                            addUser(email,username)
                            //link to login page
                            val intent = Intent(this, LoginActivity::class.java)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Password is not matching", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Empty Fields Are Not Allowed !!", Toast.LENGTH_SHORT).show()
            }
        }


        binding.textView.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    //function to add user in realtime database
    private fun addUser(email :String , username:String) {
        //create hashmap to input data
        // รับ UID ของผู้ใช้ที่ล็อกอินจาก Firebase Authentication
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val balance :Double = 0.0

        //initialize
        database = FirebaseDatabase.getInstance()
        databaseRef = database.getReference("users")

        if (uid != null) {
            val userHashmap: HashMap<String, Any> = HashMap()
            userHashmap["email"] = email
            userHashmap["username"] = username
            userHashmap["uid"] = uid  // เก็บ UID ไว้ใน Database
            userHashmap["balance"] = balance

            // เชื่อมต่อกับ Firebase Database และใช้ UID เป็นคีย์
            val database = FirebaseDatabase.getInstance()
            val databaseRef = database.getReference("users")

            databaseRef.child(uid).setValue(userHashmap)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("Firebase", "User added successfully with UID: $uid")
                    } else {
                        Log.e("Firebase", "Error adding user", task.exception)
                    }
                }
        } else {
            Log.e("Firebase", "Failed to get UID from Firebase Authentication")
        }
    }

}
