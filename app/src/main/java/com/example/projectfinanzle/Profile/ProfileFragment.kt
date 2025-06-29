package com.example.projectfinanzle.Profile

import android.content.Intent
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.projectfinanzle.LoginActivity
import com.example.projectfinanzle.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.util.Locale

class ProfileFragment : Fragment() {

    companion object {
        fun newInstance() = ProfileFragment()
    }

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var usernameTxt: TextView
    private lateinit var emailTxt: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var logoutBtn :Button
    private lateinit var tvTotalBalance3: TextView
    private lateinit var tvTotalBalance: TextView
    private lateinit var tvSavingBalance: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTotalBalance3 = view.findViewById(R.id.tvTotalBalance3)
        tvTotalBalance = view.findViewById(R.id.tvTotalBalance)
        tvSavingBalance = view.findViewById(R.id.tvSavingBalance)
        usernameTxt = view.findViewById(R.id.usernameTxt)
        emailTxt = view.findViewById(R.id.emailTxt)
        logoutBtn = view.findViewById(R.id.logoutBtn)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference.child("users")
        val user = auth.currentUser
        if (user != null) {
            val uid = user.uid
            database.child(uid).child("username").addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val displayName = snapshot.value.toString()
                        usernameTxt.text = "$displayName"
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
            emailTxt.text = user.email
            database.child(uid).child("wallets").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(walletSnapshot: DataSnapshot) {
                    var totalWalletBalance = 0.0

                    for (wallet in walletSnapshot.children) {
                        val balance = wallet.child("balance").getValue(Double::class.java) ?: 0.0
                        totalWalletBalance += balance
                    }

                    // อ่าน balance หลังจากได้ totalWalletBalance แล้ว
                    database.child(uid).child("balance").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(balanceSnapshot: DataSnapshot) {
                            val remainBalance = balanceSnapshot.getValue(Double::class.java) ?: 0.0
                            val totalAllBalance = remainBalance + totalWalletBalance
                            val totalcashbox =  totalAllBalance - remainBalance
                            val Totalbalance = remainBalance+totalcashbox
                            val formattotalbalance = NumberFormat.getNumberInstance(Locale.US).format(Totalbalance)
                            val formatRemainBalance = NumberFormat.getNumberInstance(Locale.US).format(remainBalance)
                            val formatTotalCashbox = NumberFormat.getNumberInstance(Locale.US).format(totalcashbox)


                            tvTotalBalance3.text = "$$formattotalbalance"
                            tvTotalBalance.text = "$$formatRemainBalance"
                            tvSavingBalance.text = "$$formatTotalCashbox"
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        } else {
            emailTxt.text = "No email found"
        }





        logoutBtn.setOnClickListener {

            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Confirm Logout")
            builder.setMessage("Are you sure you want to logout? This action cannot be undone.")

            // ปุ่มยืนยัน
            builder.setPositiveButton("Confirm") { dialog, which ->
                Toast.makeText(requireContext(), "You were logout", Toast.LENGTH_SHORT).show()
                val intent = Intent(requireContext() , LoginActivity::class.java)
                startActivity(intent)
            }

            // ปุ่มยกเลิก
            builder.setNegativeButton("Cancle") { dialog, which ->
                dialog.dismiss()
            }

            // แสดง Dialog
            builder.create().show()
        }
    }
}
