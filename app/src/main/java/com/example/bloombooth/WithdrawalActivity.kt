package com.example.bloombooth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.bloombooth.auth.FirebaseAuthManager.auth
import com.example.bloombooth.databinding.ActivityWithdrawalBinding
import com.google.firebase.firestore.FirebaseFirestore

class WithdrawalActivity : AppCompatActivity() {
    val binding: ActivityWithdrawalBinding by lazy {ActivityWithdrawalBinding.inflate(layoutInflater)}
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        val user = auth.currentUser
        if (user != null) {
            fetchUserData(user.uid)
        } else {
            binding.username.text = "사용자의 닉네임"
        }

        binding.checkBtn.setOnClickListener {
            binding.withdrawBtn.isEnabled = true
        }

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.withdrawBtn.setOnClickListener {
            withdrawal()
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
                    Toast.makeText(this, "사용자 데이터를 찾을 수 없습니다.",
                        Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "데이터를 가져오는 데 실패했습니다: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun withdrawal() {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            val db = FirebaseFirestore.getInstance()

            val deletedUserName = "알수없음"

            db.collection("user").document(userId)
                .delete()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        db.collection("review")
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                for (document in querySnapshot) {
                                    db.collection("reviews").document(document.id)
                                        .update("userName", deletedUserName)
                                        .addOnFailureListener { e ->
                                            Log.w("Withdrawal", "리뷰 업데이트 실패: ${e.message}")
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.w("Withdrawal", "리뷰 테이블 업데이트 중 오류 발생: ${e.message}")
                            }
                            .addOnCompleteListener {
                                user.delete()
                                    .addOnCompleteListener { deleteTask ->
                                        if (deleteTask.isSuccessful) {
                                            Toast.makeText(this, "회원 탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show()
                                            val intent = Intent(this, SplashActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                        } else {
                                            Toast.makeText(this, "Firebase Auth 계정 삭제에 실패했습니다: ${deleteTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                    } else {
                        Toast.makeText(this, "Firestore 유저 데이터 삭제에 실패했습니다: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Firestore 데이터 삭제 중 오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "로그인된 사용자가 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
