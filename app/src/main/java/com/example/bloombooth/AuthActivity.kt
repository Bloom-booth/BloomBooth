package com.example.bloombooth

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bloombooth.databinding.ActivityAuthBinding
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {

    val binding: ActivityAuthBinding by lazy { ActivityAuthBinding.inflate(layoutInflater) }
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(binding.root.id, LoginFragment())
                .commit()
        }
    }

    fun navigateToSignUpFragment() {
        supportFragmentManager.beginTransaction()
            .replace(binding.root.id, RegisterFragment())
            .addToBackStack(null)
            .commit()
    }
}
