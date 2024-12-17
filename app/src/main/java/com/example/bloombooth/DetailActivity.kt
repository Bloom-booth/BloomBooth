package com.example.bloombooth

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.bloombooth.databinding.ActivityDetailBinding

class DetailActivity : AppCompatActivity() {
    val binding : ActivityDetailBinding by lazy {ActivityDetailBinding.inflate(layoutInflater)}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        val boothId = "부스아이디"
        val boothName = "현재 가게 이름"

        binding.writeReviewBtn.setOnClickListener {
            val intent = Intent(this, ReviewActivity::class.java)
            intent.putExtra("boothName", boothName)
            intent.putExtra("boothId", boothId)
            startActivity(intent)
        }
    }
}