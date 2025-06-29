package com.example.projectfinanzle.Transfer.Payment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.projectfinanzle.HomeActivity
import com.example.projectfinanzle.Transfer.TransferFragment
import com.example.projectfinanzle.Transfer.TransferOut.TransferOutFragment
import com.example.projectfinanzle.databinding.FragmentTransferOutBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class PaymentFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var spinnerWallet: Spinner
    private lateinit var descriptionText : EditText
    private var walletArrayList: ArrayList<String> = ArrayList()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var addBtn: Button
    private lateinit var balanceInput: EditText
    private val walletIdMap = mutableMapOf<String, String>() // Map เก็บ {walletName: walletId}
    private var _binding: FragmentTransferOutBinding? = null
    private lateinit var backtotransfer:ImageView
    private lateinit var balanceUse:TextView

    companion object {
        fun newInstance(): TransferOutFragment {
            return TransferOutFragment()
        }
    }
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //val dashboardViewModel =
        //ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentTransferOutBinding.inflate(inflater, container, false)
        val root: View = binding.root
        //xml to fragment
        spinnerWallet = binding.spinnerWallet
        balanceInput = binding.balanceInput
        addBtn = binding.addBtn
        descriptionText = binding.descriptionTxt
        backtotransfer = binding.backtotransfer
        balanceUse = binding.balanceUse


        backtotransfer.setOnClickListener {
            val intent = Intent(requireContext() , TransferFragment::class.java)
            startActivity(intent)
        }

        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, walletArrayList)
        spinnerWallet.adapter = adapter

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user == null) {
            // ถ้าไม่มีผู้ใช้ที่ลงชื่อเข้าใช้ ให้แสดงข้อความหรือส่งผู้ใช้ไปที่หน้าลงชื่อเข้าใช้
            Toast.makeText(requireContext(), "กรุณาลงชื่อเข้าใช้ก่อน", Toast.LENGTH_SHORT).show()
            val intent = Intent(requireContext(), HomeActivity::class.java)
            startActivity(intent)
            return root // หยุดการทำงานของ Fragment
        }
        database = FirebaseDatabase.getInstance().reference.child("users")
        val userId = user.uid
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
            val selectedWalletName = spinnerWallet.selectedItem?.toString() ?: ""
            val walletId = walletIdMap[selectedWalletName] ?: run {
                Toast.makeText(requireContext(), "Wallet not found!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

        }
        val backImage: ImageView = binding.backtotransfer

        backImage.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }


        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



    //load all wallet name into spinner lists
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
                    adapter.notifyDataSetChanged()  // รีเฟรช spinner เมื่อข้อมูลถูกโหลดใหม่

                    // หลังจากโหลดข้อมูลเสร็จ ให้ดึงข้อมูล balance ใหม่
                    updateBalanceDisplay()
                } else {
                    Toast.makeText(requireContext(), "ไม่มีข้อมูล Wallet", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "เกิดข้อผิดพลาด: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }



    private fun calBalance() {
        val selectedWalletName = spinnerWallet.selectedItem?.toString() ?: ""
        if (selectedWalletName.isEmpty()) {
            Toast.makeText(requireContext(), "กรุณาเลือก Wallet", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        val walletId = walletIdMap[selectedWalletName]
        if (walletId == null) {
            Toast.makeText(requireContext(), "ไม่พบ Wallet นี้", Toast.LENGTH_SHORT).show()
            return
        }

        val walletRef = database.child(userId)

        walletRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(accountSnapshot: DataSnapshot) {
                val inputAmount = balanceInput.text.toString().toDoubleOrNull()

                if (inputAmount == null || inputAmount <= 0) {
                    Toast.makeText(requireContext(), "กรุณากรอกจำนวนเงินที่ถูกต้อง", Toast.LENGTH_SHORT).show()
                    return
                }

                walletRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(walletSnapshot: DataSnapshot) {
                        val walletBalance = walletSnapshot.child("wallets").child(walletId).child("balance").getValue(Double::class.java) ?: 0.0
                        val newWalletBalance = walletBalance - inputAmount

                        if (newWalletBalance < 0) {
                            Toast.makeText(requireContext(), "ยอดเงินคงเหลือไม่พอ", Toast.LENGTH_SHORT).show()
                            return
                        }else {
                            recordTransaction(userId,walletId,"expense",balanceInput.text.toString().toDouble(),descriptionText.text.toString())
                            walletRef.child("wallets").child(walletId).child("balance").setValue(newWalletBalance)
                            Toast.makeText(requireContext(), "อัปเดตยอดเงินสำเร็จ", Toast.LENGTH_SHORT).show()
                            val intent = Intent(requireContext() , HomeActivity::class.java)
                            startActivity(intent)
                        }
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


    private fun recordTransaction(userId :String , walletId :String , type :String , balance :Double , description :String) {
        val transactionRef = database.child(userId).child("wallets").child(walletId).child("transaction")
        val transactionId = transactionRef.push().key
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss" , Locale.getDefault())
        val currentDateTime = sdf.format(Date())

        val transaction = mapOf(
            "type" to type ,
            "balance" to balance ,
            "description" to description ,
            "date" to currentDateTime
        )

        transactionId?.let {
            transactionRef.child(it).setValue(transaction)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        Log.d("Firebase", "Transaction recorded successfully")
                    } else {
                        Log.e("Firebase", "Error recording transaction", it.exception)
                    }
                }
        }
    }

    private fun updateBalanceDisplay() {
        val selectedWalletName = spinnerWallet.selectedItem?.toString() ?: ""
        if (selectedWalletName.isEmpty()) {
            balanceUse.text = "No wallet selected"
            return
        }

        val userId = auth.currentUser?.uid ?: return
        val walletId = walletIdMap[selectedWalletName]

        if (walletId == null) {
            balanceUse.text = "No wallet selected"
            return
        }

        database.child(userId).child("wallets").child(walletId).child("balance")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val balanceUserData = snapshot.getValue(Double::class.java) ?: 0.0
                    balanceUse.text = "Available balance: $balanceUserData"
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "เกิดข้อผิดพลาด: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
