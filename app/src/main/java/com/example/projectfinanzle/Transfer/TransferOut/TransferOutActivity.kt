package com.example.projectfinanzle.Transfer.TransferOut

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.projectfinanzle.HomeActivity
import com.example.projectfinanzle.R
import com.example.projectfinanzle.databinding.ActivityPaymentBinding
import com.example.projectfinanzle.databinding.ActivityTransferOutBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale

class TransferOutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTransferOutBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var spinnerWallet: Spinner
    private lateinit var descriptionText: EditText
    private var walletArrayList: ArrayList<String> = ArrayList()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var addBtn: Button
    private lateinit var balanceInput: EditText
    private val walletIdMap = mutableMapOf<String, String>()
    private lateinit var backToTransfer: ImageView
    private lateinit var balanceUse: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTransferOutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        spinnerWallet = binding.spinnerWallet
        balanceInput = binding.balanceInput
        addBtn = binding.addBtn
        descriptionText = binding.descriptionTxt
        backToTransfer = binding.backtotransfer
        balanceUse = binding.balanceUse

        backToTransfer.setOnClickListener {
            finish()
        }

        adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, walletArrayList)
        spinnerWallet.adapter = adapter

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(this, "กรุณาลงชื่อเข้าใช้ก่อน", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        database = FirebaseDatabase.getInstance().reference.child("users")
        loadWalletData()

        spinnerWallet.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateBalanceDisplay()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        addBtn.setOnClickListener {
            calBalance()
        }
    }

    private fun loadWalletData() {
        val userId = auth.currentUser?.uid ?: return
        val walletRef = database.child(userId).child("wallets")

        walletRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                walletArrayList.clear()
                walletIdMap.clear()
                if (snapshot.exists()) {
                    for (walletSnapshot in snapshot.children) {
                        val walletId = walletSnapshot.key
                        val walletName = walletSnapshot.child("name").getValue(String::class.java)
                        if (walletId != null && walletName != null) {
                            walletArrayList.add(walletName)
                            walletIdMap[walletName] = walletId
                        }
                    }
                    adapter.notifyDataSetChanged()
                    updateBalanceDisplay()
                } else {
                    Toast.makeText(this@TransferOutActivity, "ไม่มีข้อมูล Wallet", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TransferOutActivity, "เกิดข้อผิดพลาด: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun calBalance() {
        val selectedWalletName = spinnerWallet.selectedItem?.toString() ?: ""
        if (selectedWalletName.isEmpty()) {
            Toast.makeText(this@TransferOutActivity, "กรุณาเลือก Wallet", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        val walletId = walletIdMap[selectedWalletName]
        if (walletId == null) {
            Toast.makeText(this@TransferOutActivity, "ไม่พบ Wallet นี้", Toast.LENGTH_SHORT).show()
            return
        }

        val walletRef = database.child(userId)

        walletRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(accountSnapshot: DataSnapshot) {
                val inputAmount = balanceInput.text.toString().toDoubleOrNull()

                if (inputAmount == null || inputAmount <= 0) {
                    Toast.makeText(this@TransferOutActivity, "กรุณากรอกจำนวนเงินที่ถูกต้อง", Toast.LENGTH_SHORT).show()
                    return
                }

                walletRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(walletSnapshot: DataSnapshot) {
                        val availableBalance = walletSnapshot.child("balance").getValue(Double::class.java) ?: 0.0
                        val walletBalance = walletSnapshot.child("wallets").child(walletId).child("balance").getValue(Double::class.java) ?: 0.0
                        val newWalletBalance = walletBalance - inputAmount
                        val newAvailableBalance = availableBalance + inputAmount

                        if (newWalletBalance < 0) {
                            Toast.makeText(this@TransferOutActivity, "ยอดเงินคงเหลือไม่พอ", Toast.LENGTH_SHORT).show()
                            return
                        }else {
                            recordTransaction(userId,walletId,"expense",balanceInput.text.toString().toDouble(),descriptionText.text.toString())
                            walletRef.child("wallets").child(walletId).child("balance").setValue(newWalletBalance)
                            walletRef.child("balance").setValue(newAvailableBalance)
                            Toast.makeText(this@TransferOutActivity, "อัปเดตยอดเงินสำเร็จ", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@TransferOutActivity , HomeActivity::class.java)
                            startActivity(intent)
                        }
                    }

                    override fun onCancelled(walletError: DatabaseError) {
                        Toast.makeText(this@TransferOutActivity, "เกิดข้อผิดพลาด: ${walletError.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            override fun onCancelled(accountError: DatabaseError) {
                Toast.makeText(this@TransferOutActivity, "เกิดข้อผิดพลาดในการดึงข้อมูลบัญชี", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun recordTransaction(userId: String, walletId: String, type: String, balance: Double, description: String) {
        val transactionRef = database.child(userId).child("wallets").child(walletId).child("transaction")
        val transactionId = transactionRef.push().key
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentDateTime = sdf.format(Date())

        val transaction = mapOf(
            "type" to type,
            "balance" to balance,
            "description" to description,
            "date" to currentDateTime
        )

        transactionId?.let {
            transactionRef.child(it).setValue(transaction)
        }
    }

    private fun updateBalanceDisplay() {
        val selectedWalletName = spinnerWallet.selectedItem?.toString() ?: return
        val userId = auth.currentUser?.uid ?: return
        val walletId = walletIdMap[selectedWalletName] ?: return

        database.child(userId).child("wallets").child(walletId).child("balance")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    balanceUse.text = "Pocket Cash Box's: ${snapshot.getValue(Double::class.java) ?: 0.0}"
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}