package com.example.bloombooth

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bloombooth.auth.FirebaseAuthManager
import com.example.bloombooth.databinding.ActivityMyReviewBinding
import com.google.firebase.firestore.FieldPath
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
            fetchReviewsFromUser(user.uid)
        } else {
            binding.username.text = "사용자의 닉네임"
        }

        binding.myreviewBackBtn.setOnClickListener {
            finish()
        }
    }

    private fun fetchUserData(userId: String) {
        db.collection("user").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nickname = document.getString("nickname") ?: "사용자의 닉네임"
                    binding.username.text = nickname

                    val reviewIds = document.get("review_ids") as? List<String> ?: emptyList()

                    if (reviewIds.isNotEmpty()) {
                        fetchReviewsByIds(reviewIds)
                    }
                } else {
                    Toast.makeText(this, "사용자 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "데이터를 가져오는 데 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchReviewsFromUser(userId: String) {
        val userRef = db.collection("user").document(userId)
        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val reviewIds = document.get("review_ids") as? List<String> ?: emptyList()
                    if (reviewIds.isNotEmpty()) {
                        fetchReviewsByIds(reviewIds)
                    } else {
                        Toast.makeText(this, "작성하신 리뷰가 없습니다!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "사용자를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "사용자 데이터를 가져오는 데 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchReviewsByIds(reviewIds: List<String>) {
        if (reviewIds.isEmpty()) return

        val fetchedReviews = mutableListOf<ReviewItem>()
        var fetchedCount = 0

        val reviewQuery = db.collection("review")
            .whereIn(FieldPath.documentId(), reviewIds)
            .orderBy("review_date", Query.Direction.DESCENDING)

        reviewQuery.get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot) {
                    val reviewText = document.getString("review_text") ?: "리뷰 내용 없음"
                    val reviewRating = document.getLong("review_rating")?.toInt() ?: 0
                    val reviewDateString = document.getString("review_date") ?: "날짜 없음"
                    val boothCnt = document.getLong("booth_cnt")?.toInt() ?: 0
                    val accsCondi = document.getLong("accs_condi")?.toInt() ?: 0
                    val accsCnt = document.getLong("accs_cnt")?.toInt() ?: 0
                    val retouching = document.getLong("retouching")?.toInt() ?: 0
                    val boothId = document.getString("booth_id") ?: ""
                    val userId = document.getString("user_id") ?: ""
                    val userName = document.getString("user_name") ?: ""
                    val photoUrls = document.get("photo_urls") as? List<String> ?: emptyList()

                    val reviewId = document.id
                    fetchBoothName(boothId) { boothName ->
                        val reviewItem = ReviewItem(
                            review_date = reviewDateString,
                            booth_cnt = boothCnt,
                            accs_condi = accsCondi,
                            accs_cnt = accsCnt,
                            retouching = retouching,
                            review_rating = reviewRating,
                            booth_id = boothId,
                            booth_name = boothName,
                            user_id = userId,
                            user_name = userName,
                            review_text = reviewText,
                            photo_urls = photoUrls,
                            review_id = reviewId
                        )

                        fetchedReviews.add(reviewItem)
                        fetchedCount++
                        if (fetchedCount == reviewIds.size) {
                            updateUIWithReviews(fetchedReviews)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "리뷰 데이터를 가져오는 데 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchBoothName(boothId: String, callback: (String) -> Unit) {
        if (boothId.isNotEmpty()) {
            db.collection("booth").document(boothId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val boothName = document.getString("booth_name") ?: "부스 이름 없음"
                        callback(boothName)
                    } else {
                        callback("부스 이름 없음")
                    }
                }
                .addOnFailureListener {
                    callback("부스 이름 없음")
                }
        } else {
            callback("부스 이름 없음")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    private fun updateUIWithReviews(fetchedReviews: MutableList<ReviewItem>) {
        setupRecyclerView(fetchedReviews)
    }

    private fun setupRecyclerView(reviews: MutableList<ReviewItem>) {
        mypageReviewAdapter = MypageReviewAdapter(this, reviews)
        binding.recyclerViewReviews.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewReviews.adapter = mypageReviewAdapter
    }
}
