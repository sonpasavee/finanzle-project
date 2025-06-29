package com.example.projectfinanzle.Home

import android.content.Intent
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.projectfinanzle.AddBalance
import com.example.projectfinanzle.Create.CreateFragment
import com.example.projectfinanzle.Create.MyAdapter
import com.example.projectfinanzle.Create.Wallets
import com.example.projectfinanzle.R
import com.example.projectfinanzle.RegisterActivity
import com.example.projectfinanzle.TransactionAdapter
import com.example.projectfinanzle.Transactions
import com.example.projectfinanzle.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var balanceAmount: TextView
    private lateinit var addBalanceBtn : Button
    private lateinit var walletRecyclerView : RecyclerView
    private lateinit var walletArrayList :ArrayList<Wallets>
    private lateinit var displaynameTxt :TextView
    private lateinit var totalBalance :TextView
    private lateinit var balanceAmount2:TextView
    private lateinit var walletRecyclerView2 :RecyclerView


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        walletRecyclerView = binding.RecyclerView
// ใช้ LinearLayoutManager แบบแนวนอน
        walletRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        walletRecyclerView.setHasFixedSize(true)

        walletRecyclerView2 = binding.RecyclerView2
// ใช้ LinearLayoutManager แบบแนวนอน
        walletRecyclerView2.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        walletRecyclerView2.setHasFixedSize(true)

//        check user
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference.child("users")
        addBalanceBtn = binding.addBalanceBtn
        addBalanceBtn.setOnClickListener {
            val intent = Intent(requireContext(), AddBalance::class.java)
            startActivity(intent)
        }
        val user = auth.currentUser
        if (user != null) {
            val uid = user.uid
            totalBalance = binding.totalBalance
            balanceAmount2 = binding.balanceAmount2
            displaynameTxt = binding.MyAccountPN

            database.child(uid).child("username").addListenerForSingleValueEvent(object : ValueEventListener {
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
                    database.child(uid).child("balance").addListenerForSingleValueEvent(object : ValueEventListener {
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

        //add balance
        balanceAmount = binding.balanceAmount2


        loadBalanceData()
        walletArrayList = arrayListOf<Wallets>()
        getWalletData("Expense")
        getWalletData("Saving")




        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadBalanceData() {
        val user = auth.currentUser
        if(user != null) {
            val uid = user.uid
            database.child(uid).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val balanceData = snapshot.child("balance").value.toString()

                        balanceAmount.text = "Your Balance : $balanceData\n"
                    } else {
                        balanceAmount.text = "ไม่พบข้อมูลผู้ใช้"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
    }

    private fun getWalletData(tag: String) {
        val user = auth.currentUser
        if (user != null) {
            val uid = user.uid
            database.child(uid).child("wallets").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // สร้างรายชื่อแยกสำหรับ Expense และ Saving
                        val expenseWalletList = ArrayList<Wallets>()
                        val savingWalletList = ArrayList<Wallets>()

                        for (walletSnapshot in snapshot.children) {
                            val walletData = walletSnapshot.getValue(Wallets::class.java)
                            walletData?.let {
                                // เพิ่มข้อมูลไปยังรายการที่ถูกต้องตาม tag
                                if (it.tag == "Expense") {
                                    expenseWalletList.add(it)
                                } else if (it.tag == "Saving") {
                                    savingWalletList.add(it)
                                } else {
                                    // กรณีที่ค่า tag ไม่ใช่ "Expense" หรือ "Saving"
                                    Log.d("WalletData", "Unknown tag: ${it.tag}")
                                }

                            }
                        }


                        // ตั้งค่า Adapter ด้วยรายการที่กรองแล้ว
                        walletRecyclerView.adapter = MyAdapter(expenseWalletList)
                        walletRecyclerView2.adapter = MyAdapter(savingWalletList)
                    } else {
                        // ถ้าข้อมูลว่าง
                        walletRecyclerView.adapter?.notifyDataSetChanged()
                        walletRecyclerView2.adapter?.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

}