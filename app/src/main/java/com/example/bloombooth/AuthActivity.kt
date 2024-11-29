package com.example.bloombooth

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
                .replace(binding.root.id, LoginFragment(), LoginFragment::class.java.simpleName)
                .commit()
        }
    }

    fun navigateToSignUpFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, RegisterFragment(), RegisterFragment::class.java.simpleName)
            .addToBackStack(null)
            .commit()
    }

    fun navigateToLoginFragment() {
        val loginFragment = LoginFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, loginFragment)
            .addToBackStack(null)  // 백스택에 추가하여 뒤로 가기 버튼을 처리할 수 있도록 함
            .commit()
    }

}
