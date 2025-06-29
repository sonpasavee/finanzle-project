package com.example.projectfinanzle

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectfinanzle.Home.HomeFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DetailWallet : AppCompatActivity() {
    private lateinit var nameWalletDetail : TextView
    private lateinit var balanceWalletDetail : TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var transactionArrayList : ArrayList<Transactions>
    private lateinit var transactionRecyclerView : RecyclerView
    private lateinit var targetAmount:TextView
    private lateinit var deleteWalletBtn:Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detail_wallet)

        // จัดการการแสดงผลของระบบให้มีการขยายไปขอบจอ
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        init()

        transactionRecyclerView = findViewById(R.id.DetailRecyclerView)
        transactionRecyclerView.layoutManager = LinearLayoutManager(this)
        transactionRecyclerView.setHasFixedSize(true)
        transactionArrayList = arrayListOf<Transactions>()


        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference.child("users")


        val walletName = intent.getStringExtra("walletName")
        val walletBalance = intent.getDoubleExtra("walletBalance", 0.0)
        val tag = intent.getStringExtra("tag")
        val targetAmountData = intent.getDoubleExtra("targetAmount" , 0.0)
        if(tag == "Saving") {
            targetAmount.visibility = View.VISIBLE
            targetAmount.text = "Target Amount : $targetAmountData"
        }else {
            targetAmount.visibility = View.GONE
        }

        nameWalletDetail.text = "$walletName"
        balanceWalletDetail.text = "Cash Box : $walletBalance"

        getTransactionData()

        val imageBack = findViewById<ImageView>(R.id.backtotransfer)
        imageBack.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()  // ปิด BackToTransfer Activity
        }


        val user = auth.currentUser
        if(user != null) {
            val uid = user.uid

            database.child(uid).child("wallets").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var walletId: String? = null
                    for (wallNameSnapshot in snapshot.children) {
                        if (wallNameSnapshot.child("name").getValue(String::class.java) == walletName) {
                            walletId = wallNameSnapshot.key
                            break
                        }
                    }

                    // ตรวจสอบ walletId ที่ได้มา
                    if (walletId != null) {
                        deleteWalletBtn.setOnClickListener {
                            val builder = AlertDialog.Builder(this@DetailWallet)
                            builder.setTitle("Confirm Deletion")
                            builder.setMessage("Are you sure you want to delete this wallet? This action cannot be undone.")

                            builder.setPositiveButton("Confirm") { dialog, which ->
                                // ดึงค่า balance ของ Wallet ที่จะลบ
                                database.child(uid).child("wallets").child(walletId).child("balance")
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(walletSnapshot: DataSnapshot) {
                                            val wBalance = walletSnapshot.getValue(Double::class.java) ?: 0.0

                                            // ดึงค่า balance ของผู้ใช้
                                            database.child(uid).child("balance").addListenerForSingleValueEvent(object : ValueEventListener {
                                                override fun onDataChange(remainingSnapshot: DataSnapshot) {
                                                    val rBalance = remainingSnapshot.getValue(Double::class.java) ?: 0.0
                                                    val afterDelBalance = wBalance + rBalance

                                                    // อัปเดต balance ของผู้ใช้
                                                    database.child(uid).child("balance").setValue(afterDelBalance)
                                                        .addOnSuccessListener {
                                                            // อัปเดตสำเร็จ -> ลบ Wallet
                                                            database.child(uid).child("wallets").child(walletId)
                                                                .removeValue()
                                                                .addOnSuccessListener {
                                                                    Toast.makeText(this@DetailWallet, "ลบวอลเล็ตเรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                                                                    finish()  // ปิด Activity หลังจากลบ Wallet
                                                                }
                                                                .addOnFailureListener {
                                                                    Toast.makeText(this@DetailWallet, "ลบวอลเล็ตไม่สำเร็จ", Toast.LENGTH_SHORT).show()
                                                                }
                                                        }
                                                        .addOnFailureListener {
                                                            Toast.makeText(this@DetailWallet, "อัปเดตยอดเงินไม่สำเร็จ", Toast.LENGTH_SHORT).show()
                                                        }
                                                }

                                                override fun onCancelled(error: DatabaseError) {
                                                    Toast.makeText(this@DetailWallet, "เกิดข้อผิดพลาดในการอ่านยอดเงิน", Toast.LENGTH_SHORT).show()
                                                }
                                            })
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            Toast.makeText(this@DetailWallet, "เกิดข้อผิดพลาดในการอ่านยอดเงินของ Wallet", Toast.LENGTH_SHORT).show()
                                        }
                                    })
                            }

                            builder.setNegativeButton("Cancel") { dialog, which ->
                                dialog.dismiss()  // ปิด Dialog
                            }

                            builder.create().show()  // แสดง Dialog
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // จัดการกรณีที่เกิดข้อผิดพลาด
                }
            })

        }


    }

    private fun init() {
        nameWalletDetail = findViewById(R.id.nameWalletDetail)
        balanceWalletDetail = findViewById(R.id.balanceWalletDetail)
        targetAmount = findViewById(R.id.targetAmount)
        deleteWalletBtn = findViewById(R.id.deleteWalletBtn)
    }

    private fun getTransactionData() {
        val walletName = intent.getStringExtra("walletName")
        val user = auth.currentUser
        var checkName: Boolean = false

        if (user != null) {
            val uid = user.uid

            database.child(uid).child("wallets").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var walletId: String? = null
                    for (walletSnapshot in snapshot.children) {
                        val walletNameRealtime = walletSnapshot.child("name").value.toString()
                        if (walletName == walletNameRealtime) {
                            walletId = walletSnapshot.key
                            checkName = true
                            break
                        }
                    }

                    if (checkName) {
                        // ดึงข้อมูล transaction จาก wallet ที่ตรงกับชื่อ
                        database.child(uid).child("wallets").child(walletId.toString()).child("transaction")
                            .addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    transactionArrayList.clear() // ล้างข้อมูลเก่าก่อน
                                    for (transactionSnapshot in snapshot.children) {
                                        val transactionData = transactionSnapshot.getValue(
                                            Transactions::class.java)
                                        transactionData?.let {
                                            transactionArrayList.add(0 , it)
                                        }
                                    }
                                    // ตั้งค่า adapter ใหม่
                                    transactionRecyclerView.adapter?.notifyItemInserted(0)
                                    transactionRecyclerView.adapter = TransactionAdapter(transactionArrayList)
                                }


                                override fun onCancelled(error: DatabaseError) {
                                    // จัดการกรณีที่เกิดข้อผิดพลาด
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // จัดการกรณีที่เกิดข้อผิดพลาด
                }

            })
        }
    }
}