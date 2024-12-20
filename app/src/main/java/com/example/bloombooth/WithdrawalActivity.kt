package com.example.bloombooth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.bloombooth.auth.FirebaseAuthManager.auth
import com.example.bloombooth.databinding.ActivityWithdrawalBinding
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseUser
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
            binding.checkBtn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.pink)
            binding.withdrawBtn.isEnabled = true
            binding.withdrawBtn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.pink)
        }

        binding.withdrawlBackBtn.setOnClickListener {
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
                    val nickname = document.getString("hickname") ?: "사용자의 닉네임"
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
            val newUserId = getString(R.string.delete_user_id)

            // 유저 데이터 가져오기
            db.collection("user").document(userId)
                .get()
                .addOnSuccessListener { userDocument ->
                    if (userDocument != null && userDocument.exists()) {
                        val reviewIds = userDocument.get("review_ids") as? List<String> ?: emptyList()

                        if (reviewIds.isNotEmpty()) {
                            // 리뷰들 업데이트
                            val updateTasks = mutableListOf<Task<Void>>()
                            for (reviewId in reviewIds) {
                                val reviewRef = db.collection("review").document(reviewId)
                                val updateTask = reviewRef.update("user_id", newUserId, "user_name", deletedUserName)
                                    .addOnFailureListener { e ->
                                        Log.w("Withdrawal", "리뷰 업데이트 실패: ${e.message}")
                                    }
                                updateTasks.add(updateTask)
                            }

                            // 모든 리뷰 업데이트 완료 후 유저 삭제
                            Tasks.whenAllSuccess<Void>(*updateTasks.toTypedArray())
                                .addOnSuccessListener {
                                    // 리뷰 업데이트 완료 후 유저 삭제
                                    deleteUser(userId, user)
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Withdrawal", "리뷰 업데이트 중 오류 발생: ${e.message}")
                                    Toast.makeText(this, "리뷰 업데이트 중 오류 발생", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            // 리뷰가 없으면 바로 사용자 삭제
                            deleteUser(userId, user)
                        }
                    } else {
                        Log.w("Withdrawal", "유저 데이터 가져오기 실패")
                        Toast.makeText(this, "유저 데이터를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("Withdrawal", "유저 데이터 가져오기 실패: ${e.message}")
                    Toast.makeText(this, "유저 데이터를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "로그인된 사용자가 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 유저 삭제 처리
    private fun deleteUser(userId: String, user: FirebaseUser) {
        val db = FirebaseFirestore.getInstance()

        // Firestore에서 유저 데이터 삭제
        db.collection("user").document(userId)
            .delete()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Firebase Auth 계정 삭제
                    user.delete()
                        .addOnCompleteListener { deleteTask ->
                            if (deleteTask.isSuccessful) {
                                Toast.makeText(this, "회원 탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, SplashActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                            } else {
                                Log.e("Withdrawal", "Firebase Auth 계정 삭제에 실패했습니다: ${deleteTask.exception?.message}")
                                Toast.makeText(this, "Firebase Auth 계정 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Log.e("Withdrawal", "Firestore 유저 데이터 삭제에 실패했습니다: ${task.exception?.message}")
                    Toast.makeText(this, "Firestore 유저 데이터 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Withdrawal", "Firestore 데이터 삭제 중 오류 발생: ${e.message}")
                Toast.makeText(this, "Firestore 데이터 삭제 중 오류 발생", Toast.LENGTH_SHORT).show()
            }
    }

}
