package com.example.bloombooth

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.bloombooth.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    val binding: ActivitySplashBinding by lazy {ActivitySplashBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        ObjectAnimator.ofFloat(binding.catchphrase, "alpha", 0f, 1f).apply {
            duration = 1600
            start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }
}