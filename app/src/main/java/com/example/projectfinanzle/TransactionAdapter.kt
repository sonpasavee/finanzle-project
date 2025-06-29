package com.example.projectfinanzle

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter(private val transactionList: ArrayList<Transactions>) : RecyclerView.Adapter<TransactionAdapter.MyViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.transaction_model, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentTransaction = transactionList[position]

        holder.description.text = currentTransaction.description
        holder.amount.text = currentTransaction.balance.toString()
        holder.date.text = currentTransaction.date

        if(currentTransaction.type == "income") {
            holder.amount.setTextColor(Color.GREEN)
            holder.amount.text = "+${currentTransaction.balance}"
        }else {
            holder.amount.setTextColor(Color.parseColor("#F44336"))
            holder.amount.text = "-${currentTransaction.balance}"
        }
    }

    override fun getItemCount(): Int {
        return transactionList.size
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val description: TextView = itemView.findViewById(R.id.description)
        val amount: TextView = itemView.findViewById(R.id.amount)
        val date: TextView = itemView.findViewById(R.id.date) // Changed Button to TextView for date
    }
}