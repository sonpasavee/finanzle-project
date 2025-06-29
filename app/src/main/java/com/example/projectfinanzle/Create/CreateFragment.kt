package com.example.projectfinanzle.Create

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.projectfinanzle.HomeActivity
import com.example.projectfinanzle.R
import com.example.projectfinanzle.databinding.FragmentCreateBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreateFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var walletName: EditText
    private lateinit var budgetWallet: EditText
    private lateinit var addWalletBtn: Button
    private lateinit var walletType: RadioGroup
    private lateinit var targetAmountEditText: EditText
    private lateinit var totalBalance: TextView

    private var _binding: FragmentCreateBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // ใช้ Fragment's View Binding
        _binding = FragmentCreateBinding.inflate(inflater, container, false)

        // Initializing views
        walletName = binding.nameWallet
        budgetWallet = binding.balanceWallet
        addWalletBtn = binding.addWalletBtn
        walletType = binding.radioGroupWalletType
        targetAmountEditText = binding.targetAmountEditText
        totalBalance = binding.totalBalance

        // Checking selected wallet type and toggling visibility of target amount input
        setupWalletTypeListener()

        // Firebase authentication
        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            database = FirebaseDatabase.getInstance().reference
            val uid = user.uid
            database.child("users").child(uid).child("balance")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val balanceData = snapshot.getValue(Double::class.java)
                            val formatBalanceData = NumberFormat.getNumberInstance(Locale.US).format(balanceData)

                            totalBalance.text = "$$formatBalanceData"
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
        // Add wallet when button clicked
        addWalletBtn.setOnClickListener {
            val walletNameText = walletName.text.toString()
            val budgetText = budgetWallet.text.toString()

            // Check if any input is empty
            if (walletNameText.isEmpty() || budgetText.isEmpty()) {
                Toast.makeText(requireContext(), "กรุณากรอกข้อมูลให้ครบถ้วน", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if user is logged in
            if (user != null) {
                val uid = user.uid
                database = FirebaseDatabase.getInstance().reference.child("users")

                // Converting budget amount to double
                val budgetAmount = budgetText.toDoubleOrNull() ?: 0.0


                // Add wallet data
                val selectedWalletTypeId = walletType.checkedRadioButtonId
                val selectedWalletTypeButton: RadioButton = binding.root.findViewById(selectedWalletTypeId)
                val selectedTypeValue = selectedWalletTypeButton.text.toString()
                val targetAmount = targetAmountEditText.text.toString().toDoubleOrNull()

                when (selectedTypeValue) {
                    "Saving" -> {
                        if (targetAmount != null) {
                            addWalletDataSaving(uid, walletNameText, budgetAmount, targetAmount)
                        } else {
                            Toast.makeText(requireContext(), "กรุณากรอกจำนวนเป้าหมายให้ถูกต้อง", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "Expense" -> {
                        addWalletDataExpense(uid, walletNameText, budgetAmount)
                    }
                    else -> {
                        Toast.makeText(requireContext(), "กรุณาเลือกประเภทกระเป๋าเงิน", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "กรุณาเข้าสู่ระบบก่อนเพิ่มกระเป๋าเงิน", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    // Function to check wallet type and adjust UI accordingly
    private fun setupWalletTypeListener() {
        val selectedWalletTypeId = walletType.checkedRadioButtonId
        val selectedWalletTypeButton: RadioButton = binding.root.findViewById(selectedWalletTypeId)
        val selectedTypeValue = selectedWalletTypeButton.text.toString()

        targetAmountEditText.visibility = if (selectedTypeValue == "Saving") {
            View.VISIBLE
        } else {
            View.GONE
        }

        walletType.setOnCheckedChangeListener { _, _ ->
            val updatedWalletTypeId = walletType.checkedRadioButtonId
            val updatedSelectedButton: RadioButton = binding.root.findViewById(updatedWalletTypeId)
            val updatedTypeValue = updatedSelectedButton.text.toString()

            targetAmountEditText.visibility = if (updatedTypeValue == "Saving") {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    // Function to add wallet data to Firebase
    private fun addWalletDataSaving(userId: String, walletName: String, initialBalance: Double, targetAmount: Double? = null) {
        val userWalletsRef = database.child(userId).child("wallets")
        val walletId = userWalletsRef.push().key
        val user = auth.currentUser

        if (user != null) {
            val uid = user.uid
            database.child(uid).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val balance = snapshot.child("balance").value.toString().toDouble()

                    val budget = budgetWallet.text.toString().toDoubleOrNull() ?: 0.0
                    if (budget <= balance) {
                        val wallet = mutableMapOf(
                            "name" to walletName,
                            "tag" to if (walletType.checkedRadioButtonId == R.id.rbExpense) "Expense" else "Saving",
                            "balance" to initialBalance
                        )

                        if (wallet["tag"] == "Saving" && targetAmount != null) {
                            wallet["targetAmount"] = targetAmount
                        }

                        walletId?.let {
                            userWalletsRef.child(it).setValue(wallet).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    recordTransaction(userId, it, "income", budget, "income")
                                    Toast.makeText(requireContext(), "เพิ่มกระเป๋าเงินสำเร็จ", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(requireContext(), "เพิ่มกระเป๋าเงินไม่สำเร็จ", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        val newBalance = balance - budget
                        database.child(uid).child("balance").setValue(newBalance)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(requireContext(), "เพิ่มยอดเงินสำเร็จ", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(requireContext(), HomeActivity::class.java)
                                    startActivity(intent)
                                    requireActivity().finish() // ปิด Fragment และกลับไปที่ HomeActivity
                                } else {
                                    Toast.makeText(requireContext(), "เกิดข้อผิดพลาดในการเพิ่มเงิน", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        Toast.makeText(requireContext(), "ยอดเงินไม่พอ", Toast.LENGTH_SHORT).show()
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "ไม่สามารถดึงข้อมูลได้", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun addWalletDataExpense(userId: String, walletName: String, initialBalance: Double) {
        val userWalletsRef = database.child(userId).child("wallets")
        val walletId = userWalletsRef.push().key
        val user = auth.currentUser

        if (user != null) {
            val uid = user.uid
            database.child(uid).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val balance = snapshot.child("balance").value.toString().toDouble()

                    val budget = budgetWallet.text.toString().toDoubleOrNull() ?: 0.0
                    if (budget <= balance) {
                        val wallet = mapOf(
                            "name" to walletName,
                            "tag" to if (walletType.checkedRadioButtonId == R.id.rbExpense) "Expense" else "Saving",
                            "balance" to initialBalance
                        )

                        walletId?.let {
                            userWalletsRef.child(it).setValue(wallet).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    recordTransaction(userId, it, "income", budget, "income")
                                    Toast.makeText(requireContext(), "เพิ่มกระเป๋าเงินสำเร็จ", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(requireContext(), "เพิ่มกระเป๋าเงินไม่สำเร็จ", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        val newBalance = balance - budget
                        database.child(uid).child("balance").setValue(newBalance)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(requireContext(), "เพิ่มยอดเงินสำเร็จ", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(requireContext(), HomeActivity::class.java)
                                    startActivity(intent)
                                    requireActivity().finish() // ปิด Fragment และกลับไปที่ HomeActivity
                                } else {
                                    Toast.makeText(requireContext(), "เกิดข้อผิดพลาดในการเพิ่มเงิน", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        Toast.makeText(requireContext(), "ยอดเงินไม่พอ", Toast.LENGTH_SHORT).show()
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "ไม่สามารถดึงข้อมูลได้", Toast.LENGTH_SHORT).show()
            }
        }
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

    // Clean up binding to avoid memory leaks
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
