package com.example.projectfinanzle
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.projectfinanzle.databinding.ActivityPaymentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.util.Locale

class AddBalance : AppCompatActivity() {
    private lateinit var addBtn :Button
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var budget: EditText
    private lateinit var displaynameTxt : TextView
    private lateinit var totalBalance : TextView
    private lateinit var balanceAmount2: TextView
    private lateinit var backtohome: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_add_balance)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        init()

        val btnExit = findViewById<ImageView>(R.id.backtohome)
        btnExit.setOnClickListener {
            finish()
        }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference.child("users")
        addBtn.setOnClickListener {
            updateBalance(budget.text.toString().toDouble())
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
        val user = auth.currentUser
        if (user != null) {
            val uid = user.uid

            database.child(uid).child("username").addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val displayName = snapshot.value.toString()
                        displaynameTxt.text = "Hello, $displayName"
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            database.child(uid).child("wallets").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(walletSnapshot: DataSnapshot) {
                    var totalWalletBalance = 0.0

                    for (wallet in walletSnapshot.children) {
                        val balance = wallet.child("balance").getValue(Double::class.java) ?: 0.0
                        totalWalletBalance += balance
                    }

                    // อ่าน balance หลังจากได้ totalWalletBalance แล้ว
                    database.child(uid).child("balance").addListenerForSingleValueEvent(object :
                        ValueEventListener {
                        override fun onDataChange(balanceSnapshot: DataSnapshot) {
                            val remainBalance = balanceSnapshot.getValue(Double::class.java) ?: 0.0
                            val totalAllBalance = remainBalance + totalWalletBalance
                            val totalcashbox =  totalAllBalance - remainBalance
                            val formatRemainBalance = NumberFormat.getNumberInstance(Locale.US).format(remainBalance)
                            val formatTotalCashbox = NumberFormat.getNumberInstance(Locale.US).format(totalcashbox)


                            balanceAmount2.text = "$$formatTotalCashbox"
                            totalBalance.text = "$$formatRemainBalance"
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }

    }



    private fun init() {
        addBtn = findViewById(R.id.addBtn)
        budget = findViewById(R.id.addBudget)
        totalBalance = findViewById(R.id.totalBalance)
        balanceAmount2 = findViewById(R.id.balanceAmount2)
        displaynameTxt = findViewById(R.id.My_AccountPN)

    }

    private fun updateBalance(budget: Double) {
        val user = auth.currentUser
        if (user != null) {
            val uid = user.uid
            database.child(uid).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val balance = snapshot.child("balance").value.toString().toDouble()

                    val newBalance = balance + budget

                    database.child(uid).child("balance").setValue(newBalance)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // แสดงข้อความแจ้งเตือนหรือทำอะไรที่ต้องการเมื่ออัปเดตเสร็จ
                                Toast.makeText(this, "เพิ่มยอดเงินสำเร็จ", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "เกิดข้อผิดพลาดในการเพิ่มเงิน", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }.addOnFailureListener {
                Toast.makeText(this, "ไม่สามารถดึงข้อมูลได้", Toast.LENGTH_SHORT).show()
            }
        }
    }

}