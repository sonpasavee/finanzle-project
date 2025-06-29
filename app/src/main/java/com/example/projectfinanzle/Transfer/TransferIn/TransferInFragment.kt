package com.example.projectfinanzle.Transfer.TransferIn

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.projectfinanzle.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class TransferInFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var spinnerWallet: Spinner
    private lateinit var balanceInput: EditText
    private lateinit var addBtn: Button
    private lateinit var adapter: ArrayAdapter<String>
    private val walletArrayList: ArrayList<String> = ArrayList()
    private val walletIdMap = mutableMapOf<String, String>() // {walletName: walletId}

    companion object {
        fun newInstance(): TransferInFragment {
            return TransferInFragment()
        }
    }

    private val viewModel: TransferInViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_transfer_in, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference.child("users")

        // UI Elements
        spinnerWallet = view.findViewById(R.id.spinnerWallet)
        balanceInput = view.findViewById(R.id.balanceInput)
        addBtn = view.findViewById(R.id.addBtn)
        val backImage: ImageView = view.findViewById(R.id.backtotransfer)

        // ตั้งค่าให้กดปุ่มย้อนกลับแล้วกลับไปหน้า Fragment ก่อนหน้า
        backImage.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // ตั้งค่า Spinner
        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, walletArrayList)
        spinnerWallet.adapter = adapter

        loadWalletData() // โหลดข้อมูล Wallet จาก Firebase

        addBtn.setOnClickListener {
            transferInBalance()
        }

        return view
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
                } else {
                    Toast.makeText(requireContext(), "ไม่มีข้อมูล Wallet", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "เกิดข้อผิดพลาด: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun transferInBalance() {
        val selectedWalletName = spinnerWallet.selectedItem?.toString() ?: ""
        val inputAmount = balanceInput.text.toString().toDoubleOrNull()

        if (selectedWalletName.isEmpty() || inputAmount == null || inputAmount <= 0) {
            Toast.makeText(requireContext(), "กรุณาเลือก Wallet และกรอกจำนวนเงินที่ถูกต้อง", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        val walletId = walletIdMap[selectedWalletName] ?: return

        val walletRef = database.child(userId).child("wallets").child(walletId)
        val accountBalanceRef = database.child(userId).child("balance")

        accountBalanceRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(accountSnapshot: DataSnapshot) {
                val accountBalance = accountSnapshot.getValue(Double::class.java) ?: 0.0

                if (accountBalance < inputAmount) {
                    Toast.makeText(requireContext(), "ยอดเงินคงเหลือไม่พอ", Toast.LENGTH_SHORT).show()
                    return
                }

                walletRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(walletSnapshot: DataSnapshot) {
                        val walletBalance = walletSnapshot.child("balance").getValue(Double::class.java) ?: 0.0
                        val newWalletBalance = walletBalance + inputAmount
                        val newAccountBalance = accountBalance - inputAmount

                        database.child(userId).child("balance").setValue(newAccountBalance)
                        walletRef.child("balance").setValue(newWalletBalance)

                        recordTransaction(userId, walletId, "income", inputAmount, "Transfer In")

                        Toast.makeText(requireContext(), "โอนเงินเข้าวอลเล็ตสำเร็จ", Toast.LENGTH_SHORT).show()
                    }

                    override fun onCancelled(walletError: DatabaseError) {
                        Toast.makeText(requireContext(), "เกิดข้อผิดพลาด: ${walletError.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            override fun onCancelled(accountError: DatabaseError) {
                Toast.makeText(requireContext(), "เกิดข้อผิดพลาดในการดึงข้อมูลบัญชี", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun recordTransaction(userId: String, walletId: String, type: String, balance: Double, description: String) {
        val transactionRef = database.child(userId).child("wallets").child(walletId).child("transaction")
        val transactionId = transactionRef.push().key
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val currentDateTime = sdf.format(java.util.Date())

        val transaction = mapOf(
            "type" to type,
            "balance" to balance,
            "description" to description,
            "date" to currentDateTime
        )

        transactionId?.let {
            transactionRef.child(it).setValue(transaction)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        android.util.Log.d("Firebase", "Transaction recorded successfully")
                    } else {
                        android.util.Log.e("Firebase", "Error recording transaction", it.exception)
                    }
                }
        }
    }
}
