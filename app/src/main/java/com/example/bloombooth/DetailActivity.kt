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

        binding.writeReviewBtn.setOnClickListener {
            val intent = Intent(this, ReviewActivity::class.java)
            intent.putExtra("boothName", "현재 가게 이름")  // value에 가게 이름 넣어주세요!
            startActivity(intent)
        }
    }
}