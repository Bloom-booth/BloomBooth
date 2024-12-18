package com.example.bloombooth

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bloombooth.auth.FirebaseAuthManager
import com.example.bloombooth.databinding.ActivityMyReviewBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MyReviewActivity : AppCompatActivity() {
    private val binding: ActivityMyReviewBinding by lazy { ActivityMyReviewBinding.inflate(layoutInflater) }
    private val db = FirebaseFirestore.getInstance()
    private lateinit var mypageReviewAdapter: MypageReviewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(binding.root)

        val user = FirebaseAuthManager.auth.currentUser
        if (user != null) {
            fetchUserData(user.uid)
            fetchUserReviews(user.uid)
        } else {
            binding.username.text = "사용자의 닉네임"
        }

        binding.myreviewBackBtn.setOnClickListener {
            finish()
        }
    }

    private fun fetchUserReviews(userId: String) {
        Log.d("test", userId)
        db.collection("review")
            .whereEqualTo("user_id", userId)
            .orderBy("review_date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val reviews = mutableListOf<ReviewItem>()
                for (document in documents) {
                    val review = document.toObject(ReviewItem::class.java)
                    reviews.add(review)
                }

                setupRecyclerView(reviews)
            }
            .addOnFailureListener { e ->
                showToast("리뷰를 불러오는데 실패했습니다: ${e.message}")
                Log.d("YEONJAE", e.message.toString())
            }
    }

    private fun fetchUserData(userId: String) {
        db.collection("user").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nickname = document.getString("nickname") ?: "사용자의 닉네임"
                    binding.username.text = nickname

                    val reviewIds = document.get("reviewIds") as? List<String> ?: emptyList()

                    if (reviewIds.isNotEmpty()) {
                        fetchReviewsByIds(reviewIds)
                    } else {
                        Toast.makeText(this, "등록된 리뷰가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "사용자 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "데이터를 가져오는 데 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchReviewsByIds(reviewIds: List<String>) {
        val reviewRef = db.collection("review")

        for (reviewId in reviewIds) {
            reviewRef.document(reviewId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val reviewText = document.getString("review_text") ?: "리뷰 내용 없음"
                        val reviewRating = document.getLong("review_rating")?.toInt() ?: 0

                        val reviewDateString = document.getString("review_date") ?: "날짜 없음"
                    } else {
                        Toast.makeText(this, "리뷰 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "리뷰 데이터를 가져오는 데 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }




    private fun setupRecyclerView(reviews: List<ReviewItem>) {
        mypageReviewAdapter = MypageReviewAdapter(this, reviews)
        binding.recyclerViewReviews.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewReviews.adapter = mypageReviewAdapter
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
