package com.example.bloombooth

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bloombooth.auth.FirebaseAuthManager
import com.example.bloombooth.databinding.ActivityMyReviewBinding
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class MyReviewActivity : AppCompatActivity() {
    private val binding: ActivityMyReviewBinding by lazy { ActivityMyReviewBinding.inflate(layoutInflater) }
    private val db = FirebaseFirestore.getInstance()
    private lateinit var mypageReviewAdapter: MypageReviewAdapter

    private val reviewList = mutableListOf<Review>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupRecyclerView()

        val user = FirebaseAuthManager.auth.currentUser
        if (user != null) {
            fetchUserData(user.uid)
        } else {
            binding.username.text = "Unknown"
        }

        val userId = FirebaseAuthManager.auth.currentUser?.uid
        if (userId != null) {
            db.collection("review")
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener { documents ->
                    reviewList.clear() // 기존 데이터 초기화
                    val tasks = mutableListOf<Task<DocumentSnapshot>>()

                    for (document in documents) {
                        val review = document.toObject(Review::class.java)
                        review.review_id = document.id

                        // booth_name 찾기
                        val task = db.collection("booth").document(review.booth_id).get()
                        tasks.add(task)

                        task.addOnSuccessListener { reviewedBooth ->
                            if (reviewedBooth.exists()) {
                                review.booth_name = reviewedBooth.getString("booth_name").toString()
                            }
                        }.addOnFailureListener {
                            review.booth_name = "Unknown"
                        }

                        if (!reviewList.contains(review)) {
                            reviewList.add(review)
                        }
                    }

                    // 모든 booth_name 로드 완료 후 UI 업데이트
                    Tasks.whenAll(tasks).addOnCompleteListener {
                        mypageReviewAdapter.notifyDataSetChanged()
                    }
                }
        }

        // 뒤로가기
        binding.header.btnBack.setOnClickListener {
            finish()
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

    private fun setupRecyclerView() {
        mypageReviewAdapter = MypageReviewAdapter(this, reviewList)
        binding.recyclerViewReviews.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewReviews.adapter = mypageReviewAdapter
    }
}