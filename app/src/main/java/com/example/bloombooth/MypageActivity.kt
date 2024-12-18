package com.example.bloombooth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.bloombooth.auth.FirebaseAuthManager
import com.example.bloombooth.databinding.ActivityMypageBinding
import com.google.firebase.firestore.FirebaseFirestore

class MypageActivity : AppCompatActivity() {
    val binding: ActivityMypageBinding by lazy { ActivityMypageBinding.inflate(layoutInflater) }
    private val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuthManager.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        binding.reviewBtn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.pink)
        binding.updateUserinfoBtn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.pink)
        binding.bookmarkBtn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.pink)
        binding.logoutBtn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.light_grey)
        binding.withdrawBtn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.light_grey)

        val user = FirebaseAuthManager.auth.currentUser
        if (user != null) {
            fetchUserData(user.uid)
        } else {
            binding.username.text = "사용자의 닉네임"
        }

        binding.homeBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding.reviewBtn.setOnClickListener {
            val intent = Intent(this, MyReviewActivity::class.java)
            startActivity(intent)
        }

        binding.updateUserinfoBtn.setOnClickListener {
            val intent = Intent(this, PersonalInfoActivity::class.java)
            startActivity(intent)
        }

        binding.bookmarkBtn.setOnClickListener {
            val intent = Intent(this, BookmarkActivity::class.java)
            startActivity(intent)
        }

        binding.withdrawBtn.setOnClickListener {
            val intent = Intent(this, WithdrawalActivity::class.java)
            startActivity(intent)
        }

        binding.logoutBtn.setOnClickListener() {
            logoutAndGotoSplash();
        }
    }

    private fun fetchUserData(userId: String) {
        db.collection("user").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nickname = document.getString("nickname") ?: "사용자의 닉네임"
                    binding.username.text = nickname
                } else {
                    Toast.makeText(this, "사용자 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "데이터를 가져오는 데 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logoutAndGotoSplash() {
        auth.signOut()
        val intent = Intent(this, SplashActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}