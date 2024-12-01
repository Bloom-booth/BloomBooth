package com.example.bloombooth

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.bloombooth.databinding.ActivityWithdrawalBinding

class WithdrawalActivity : AppCompatActivity() {
    val binding: ActivityWithdrawalBinding by lazy {ActivityWithdrawalBinding.inflate(layoutInflater)}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        binding.backBtn.setOnClickListener {
            finish()
        }
    }
}