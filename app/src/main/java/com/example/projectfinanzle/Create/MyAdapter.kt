package com.example.projectfinanzle.Create

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.projectfinanzle.DetailWallet
import com.example.projectfinanzle.R
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import java.text.NumberFormat
import java.util.Locale

class MyAdapter(private val walletList :ArrayList<Wallets>) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyAdapter.MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.wallet_model,
            parent,false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyAdapter.MyViewHolder, position: Int) {
        val currentItems = walletList[position]
        holder.walletName.text = currentItems.name

        // การแสดงผลยอดเงิน
        val formatWalletBalance = NumberFormat.getNumberInstance(Locale.US).format(currentItems.balance ?: 0.0)
        holder.walletBalance.text = formatWalletBalance

        // การแสดงผล ProgressBar สำหรับประเภท "saving"
        if (currentItems.tag == "Saving") {
            val targetAmount = currentItems.targetAmount ?: 0.0 // ใช้ 0.0 หาก targetAmount เป็น null
            val currentBalance = currentItems.balance ?: 0.0 // ใช้ 0.0 หาก balance เป็น null

            val percentage = ((currentBalance/targetAmount) * 100)
            holder.progressPercentage.text = "$percentage%"

            val progressColor = when {
                percentage >= 75 -> Color.parseColor("#4CAF50") // เขียว
                percentage >= 50 -> Color.parseColor("#FFEB3B") // เหลือง
                else -> Color.parseColor("#F44336") // แดง
            }

            val drawable = holder.progressSaving.progressDrawable.mutate() as LayerDrawable
            val progressLayer = drawable.findDrawableByLayerId(android.R.id.progress)
            progressLayer.setTint(progressColor)
            holder.progressSaving.progressDrawable = drawable

            // คำนวณ progress
            val progress = if (targetAmount > 0) {
                // ตรวจสอบว่า targetAmount > 0 ก่อนการคำนวณ
                ((currentBalance * 100) / targetAmount).toInt()
            } else {
                holder.progressPercentage.text = "0%"
                0
            }



            // แสดงผล ProgressBar
            holder.progressSaving.visibility = View.VISIBLE
            holder.progressPercentage.visibility = View.VISIBLE
            holder.progressSaving.progress = progress

        } else {
            holder.progressSaving.visibility = View.GONE
            holder.progressPercentage.visibility = View.GONE
        }

        // การตั้งค่าปุ่มเพื่อไปที่หน้ารายละเอียด
        holder.detailBtn.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DetailWallet::class.java)
            intent.putExtra("walletName", currentItems.name)
            intent.putExtra("walletBalance", currentItems.balance)
            intent.putExtra("tag" , currentItems.tag)
            intent.putExtra("targetAmount" , currentItems.targetAmount)

            context.startActivity(intent)
        }
    }


    override fun getItemCount(): Int {
        return walletList.size
    }

    class MyViewHolder(itemView : View) :RecyclerView.ViewHolder(itemView) {
        val walletName : TextView = itemView.findViewById(R.id.walletName)
        val walletBalance : TextView = itemView.findViewById(R.id.walletBalance)
        val detailBtn : Button = itemView.findViewById(R.id.detailBtn)
        val progressSaving :ProgressBar = itemView.findViewById(R.id.progressSaving)
        val progressPercentage :TextView = itemView.findViewById(R.id.progressPercentage)
    }
}