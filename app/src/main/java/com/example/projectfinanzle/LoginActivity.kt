package com.example.projectfinanzle


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.projectfinanzle.RegisterActivity
import com.example.projectfinanzle.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ใช้ Binding แทนการเรียก setContentView ด้วย layout ทั่วไป
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)  // ตั้งค่าให้กับ layout

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // ตั้งค่า OnClickListener สำหรับปุ่ม Login
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmailPhone.text.toString()
            val password = binding.passwordEdt.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                    if (it.isSuccessful) {
                        val intent = Intent(this, HomeActivity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Empty Fields Are Not Allowed !!", Toast.LENGTH_SHORT).show()
            }
        }

        // ตั้งค่า OnClickListener สำหรับไปยัง RegisterActivity
        binding.textViewS.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

