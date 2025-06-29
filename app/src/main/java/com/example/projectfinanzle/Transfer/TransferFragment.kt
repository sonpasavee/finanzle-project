package com.example.projectfinanzle.Transfer

import android.content.Intent
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.projectfinanzle.HomeActivity
import com.example.projectfinanzle.R
import com.example.projectfinanzle.Transfer.Payment.PaymentActivity
import com.example.projectfinanzle.Transfer.Payment.PaymentFragment
import com.example.projectfinanzle.Transfer.TransferBTW.TransferBTWFragment
import com.example.projectfinanzle.Transfer.TransferIn.TransferInActivity
import com.example.projectfinanzle.Transfer.TransferIn.TransferInFragment
import com.example.projectfinanzle.Transfer.TransferOut.TransferOutActivity
import com.example.projectfinanzle.Transfer.TransferOut.TransferOutFragment
import com.example.projectfinanzle.databinding.FragmentTransferBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.util.Locale

class TransferFragment : Fragment() {
    private lateinit var cashboxData :TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    companion object {
        fun newInstance() = TransferFragment()
    }

    private val viewModel: TransferViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentTransferBinding.inflate(inflater, container, false)

        cashboxData = binding.cashboxData

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user != null) {
            database = FirebaseDatabase.getInstance().reference
            val uid = user.uid
            database.child("users").child(uid).child("wallets")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            var totalWalletBalance = 0.0
                            for(snapshotBalance in snapshot.children) {
                                val walletBalance = snapshotBalance.child("balance").getValue(Double::class.java) ?: 0.0
                                totalWalletBalance += walletBalance
                            }
                            val formatTotalWalletBalance = NumberFormat.getNumberInstance(Locale.US).format(totalWalletBalance)

                            cashboxData.text = "$$formatTotalWalletBalance"
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
        // ตั้งค่าปุ่มต่างๆ

        binding.button1.setOnClickListener {
            Log.d("DEBUG", "Button 1 Clicked: Opening TransferInActivity")
            val intent = Intent(requireContext(), TransferInActivity::class.java)
            startActivity(intent)
        }

        binding.button2.setOnClickListener {
            val intent = Intent(context, TransferOutActivity::class.java)
            startActivity(intent)
        }

        binding.button4.setOnClickListener {
            val intent = Intent(context, PaymentActivity::class.java)
            startActivity(intent)
        }

        return binding.root
    }
}