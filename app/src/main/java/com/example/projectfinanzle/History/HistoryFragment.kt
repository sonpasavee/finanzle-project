package com.example.projectfinanzle.History

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.example.projectfinanzle.databinding.FragmentHistoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.NumberFormat
import java.util.Locale

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var pieChart: PieChart
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var tvTotalBalance:TextView
    private lateinit var tvIncome:TextView
    private lateinit var tvExpense:TextView


    private val viewModel: HistoryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)

        pieChart = binding.chartView
        auth = FirebaseAuth.getInstance()

        setupExpensePieChart()

        //binding element in xml
        tvTotalBalance = binding.tvTotalBalance
        tvIncome = binding.tvIncome
        tvExpense = binding.tvExpense
        //database setup
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference.child("users")

        val user = auth.currentUser
        if(user != null) {
            val uid = user.uid

            database.child(uid).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists()) {
                        var totalExpense = 0.0
                        var totalIncome = 0.0

                        val totalBalance = snapshot.child("balance").getValue(Double::class.java)
                        val formatTotalBalance = NumberFormat.getNumberInstance(Locale.US).format(totalBalance)
                        tvTotalBalance.text = "$$formatTotalBalance"

                        val userWalletsRef = database.child(uid).child("wallets")
                        userWalletsRef.addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                var totalIncome = 0.0
                                var totalExpense = 0.0

                                for (walletSnapshot in snapshot.children) {
                                    val transactionsSnapshot = walletSnapshot.child("transaction")

                                    for (transactionSnapshot in transactionsSnapshot.children) {
                                        val type = transactionSnapshot.child("type").getValue(String::class.java)
                                        val amount = transactionSnapshot.child("balance").getValue(Double::class.java) ?: 0.0

                                        if (type == "income") {
                                            totalIncome += amount
                                        }else if(type == "expense") {
                                            totalExpense += amount
                                        }
                                    }
                                }

                                Log.d("TotalIncome", "Total Income: $totalIncome")
                                Log.d("TotalIncome", "Total Expense: $totalExpense")

                                val formatTotalIncome = NumberFormat.getNumberInstance(Locale.US).format(totalIncome)
                                val formatTotalExpense = NumberFormat.getNumberInstance(Locale.US).format(totalExpense)
                                tvIncome.text = "$$formatTotalIncome"
                                tvExpense.text = "$$formatTotalExpense"                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("FirebaseError", "Failed to read data", error.toException())
                            }
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Prevent memory leaks
    }

    // Set up Pie Chart with data from Firebase
    private fun setupExpensePieChart() {
        val userId = auth.currentUser?.uid ?: return
        var totalExpenseBalance = 0.0
        var totalSavingBalance = 0.0

        // Fetch data from Firebase
        database = FirebaseDatabase.getInstance().getReference("users").child(userId)

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val entries = ArrayList<PieEntry>()

                // Check data in "wallets" of the user
                val walletsSnapshot = snapshot.child("wallets")

                // Loop through each wallet the user has
                for (walletSnapshot in walletsSnapshot.children) {
                    val walletBalance = walletSnapshot.child("balance").getValue(Double::class.java)
                    val expenseTag = walletSnapshot.child("tag").getValue(String::class.java)

                    // Calculate total balances for Expense and Saving categories
                    if (walletBalance != null) {
                        if (expenseTag == "Expense") {
                            totalExpenseBalance += walletBalance
                        } else if (expenseTag == "Saving") {
                            totalSavingBalance += walletBalance
                        }
                    }
                }

                // Fetch availableBalance (total balance in the user's account)
                val availableBalance = snapshot.child("balance").getValue(Double::class.java) ?: 0.0

                // Add total data for Expense, Saving, and Available Balance
                if (totalExpenseBalance > 0) {
                    entries.add(PieEntry(totalExpenseBalance.toFloat(), "Total Expense"))
                }
                if (totalSavingBalance > 0) {
                    entries.add(PieEntry(totalSavingBalance.toFloat(), "Total Saving"))
                }
                if (availableBalance > 0) {
                    entries.add(PieEntry(availableBalance.toFloat(), "Available Balance"))
                }

                // Show the pie chart
                showPieChart(entries)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Display the Pie Chart with data
    // Display the Pie Chart with data
    private fun showPieChart(entries: List<PieEntry>) {
        val colors = listOf(
            ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark),
            ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark),
            ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)
        )

        val dataSet = PieDataSet(entries, "Financial Overview")
        dataSet.colors = colors
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), android.R.color.black)

        // ✅ ทำให้ Pie Chart ดูโค้งมนขึ้น
        dataSet.sliceSpace = 8f // เพิ่มช่องว่างระหว่าง slice
        dataSet.selectionShift = 12f // ขยาย slice เมื่อเลือก

        // แสดงค่าตัวเลขไว้นอกชาร์ต
        dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

        val data = PieData(dataSet)
        pieChart.data = data

        // ✅ เพิ่มขนาดของ Hole ให้ Pie Chart ดูเหมือน Doughnut Chart
        pieChart.setHoleRadius(50f) // ปรับขนาดวงตรงกลางให้ใหญ่ขึ้น
        pieChart.setTransparentCircleRadius(55f) // เพิ่มวงโปร่งใสให้ดู smooth

        // ปรับแต่ง Label
        pieChart.setEntryLabelColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        pieChart.setEntryLabelTextSize(12f)

        // ซ่อน Description
        pieChart.description.isEnabled = false

        // เปิด Legend
        pieChart.legend.isEnabled = true

        // เพิ่ม Animation
        pieChart.animateY(1000)

        pieChart.invalidate() // Refresh chart
    }


}
