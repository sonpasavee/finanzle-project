package com.example.projectfinanzle.Transfer.TransferBTW

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import com.example.projectfinanzle.R

class TransferBTWFragment : Fragment() {

    companion object {
        fun newInstance() = TransferBTWFragment()
    }

    private val viewModel: TransferBTWViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_transfer_b_t_w, container, false)

        // หา ImageView ที่จะใช้ในการย้อนกลับ
        val backImage: ImageView = view.findViewById(R.id.backtotransfer)

        // ตั้งค่าให้คลิกที่รูปภาพแล้วย้อนกลับไปหน้า Fragment ก่อนหน้า
        backImage.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        return view
    }
}