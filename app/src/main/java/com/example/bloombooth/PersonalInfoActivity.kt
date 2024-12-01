package com.example.bloombooth

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bloombooth.databinding.ActivityPersonalInfoBinding

class PersonalInfoActivity : AppCompatActivity() {
    val binding: ActivityPersonalInfoBinding by lazy {ActivityPersonalInfoBinding.inflate(layoutInflater)}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        binding.backBtn.setOnClickListener {
            finish()
        }
    }
}