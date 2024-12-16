package com.example.bloombooth

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.bloombooth.databinding.ActivityThreeReviewBinding

class ThreeReviewActivity : AppCompatActivity() {
    val binding : ActivityThreeReviewBinding by lazy {ActivityThreeReviewBinding.inflate(layoutInflater)}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
    }
}