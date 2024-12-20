package com.example.bloombooth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bloombooth.auth.FirebaseAuthManager
import com.example.bloombooth.databinding.ActivityMypageBinding
import com.google.firebase.firestore.FirebaseFirestore

class MypageActivity : AppCompatActivity() {
    val binding: ActivityMypageBinding by lazy { ActivityMypageBinding.inflate(layoutInflater) }
    private val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuthManager.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val user = FirebaseAuthManager.auth.currentUser
        if (user != null) {
            fetchUserData(user.uid)
        } else {
            binding.username.text = "사용자의 닉네임"
        }

        // 메인 화면으로
        binding.header.homeBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        // 개인정보 수정하기
        binding.btnUpdateInfo.setOnClickListener {
            val intent = Intent(this, PersonalInfoActivity::class.java)
            startActivity(intent)
        }
        // 내 리뷰
        binding.btnMyReview.setOnClickListener {
            val intent = Intent(this, MyReviewActivity::class.java)
            startActivity(intent)
        }
        // 북마크
        binding.btnMyBookmark.setOnClickListener {
            val intent = Intent(this, BookmarkActivity::class.java)
            startActivity(intent)
        }
        // 로그아웃
        binding.btnLogout.setOnClickListener() {
            logoutAndGotoSplash();
        }
        // 탈퇴하기
        binding.btnWithdrawal.setOnClickListener {
            val intent = Intent(this, WithdrawalActivity::class.java)
            startActivity(intent)
        }
    }
    // 사용자 정보 불러오기
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
    // 로그아웃하고 스플래시로 이동하기
    private fun logoutAndGotoSplash() {
        auth.signOut()
        val intent = Intent(this, SplashActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}