package com.example.bloombooth

import Booth
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bloombooth.auth.FirebaseAuthManager
import com.example.bloombooth.databinding.ActivityDetailBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class DetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding
    private lateinit var adapter: BoothReviewAdapter

    private var boothNumber: Int = 0
    private var boothName: String = ""
    private var boothId: String = ""

    private val reviewList = mutableListOf<Review>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // booth 정보 초기화
        boothNumber = intent.getIntExtra("boothNumber", 0)
        boothName = intent.getStringExtra("boothName").toString()
        boothId = intent.getStringExtra("boothId").toString()

        // Adapter 초기화
        adapter = BoothReviewAdapter()
        binding.boothReview.layoutManager = LinearLayoutManager(this)
        binding.boothReview.adapter = adapter

        // 뒤로가기
        binding.icBack.setOnClickListener { finish() }

        // 북마크 상태 확인
        checkBookmarkStatus()

        // 북마크 아이콘 클릭 리스너
        binding.icBookmark.setOnClickListener {
            toggleBookmarkStatus()
        }

        // 리뷰 작성하기
        binding.writeReview.setOnClickListener {
            val intent = Intent(this, ReviewActivity::class.java)
            intent.putExtra("boothName", boothName)
            intent.putExtra("boothId", boothId)
            startActivity(intent)
        }

        // 부스 찾기
        findBooth()
    }

    private fun findBooth() {
        val db = FirebaseFirestore.getInstance()
        db.collection("booth").document(boothId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val booth = document.toObject(Booth::class.java)
                    val boothId = document.id
                    findReview(boothId)

                    if (booth != null) {
                        binding.boothName.text = booth.booth_name
                        binding.boothAddr.text = booth.booth_addr
                        binding.boothInfo.text = booth.booth_info
                        binding.boothContact.text = booth.booth_contact
                        binding.boothCnt.text = booth.booth_cnt.toString()
                        binding.accsCondi.text = goodOrBad(booth.accs_condi)
                        binding.accsCnt.text = lessOrLot(booth.accs_cnt)
                        binding.retouching.text = goodOrBad(booth.retouching)
                        binding.reviewAvg.text = booth.review_avg.toString()
                    }
                } else {
                    Log.e("DetailActivityTest", "Booth Not Found")
                }
            }
            .addOnFailureListener { e ->
                Log.e("DetailActivityTest", "Failed to Find Booth", e)
            }
    }

    private fun findReview(boothId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("review")
            .whereEqualTo("booth_id", boothId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
                        val review = document.toObject(Review::class.java)
                        // 중복된 리뷰가 없는지 확인하고 추가
                        if (!reviewList.contains(review)) {
                            reviewList.add(review)
                        }
                    }
                    sortDate()
                    updateUI(reviewList)
                }
            }
            .addOnFailureListener { e ->
                Log.e("DetailActivityTest", "Failed to Find Reviews", e)
            }
    }

    private fun sortDate() {
        val dateFormat = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())

        reviewList.sortWith { review1, review2 ->
            val date1 = dateFormat.parse(review1.review_date.toString())
            val date2 = dateFormat.parse(review2.review_date.toString())

            when {
                date1 == null -> 1
                date2 == null -> -1
                else -> date2.compareTo(date1)
            }
        }
    }

    private fun updateUI(reviewList: List<Review>) {
        adapter.submitList(reviewList)
    }

    private fun goodOrBad(value: Int): String {
        return when (value) {
            1 -> "없음"
            2 -> "나쁨"
            3 -> "보통"
            4 -> "좋음"
            else -> "미정"
        }
    }

    private fun lessOrLot(value: Int): String {
        return when (value) {
            1 -> "없음"
            2 -> "적음"
            3 -> "보통"
            4 -> "많음"
            else -> "미정"
        }
    }

    private fun checkBookmarkStatus() {
        val user = FirebaseAuthManager.auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("user").document(user.uid)

            userRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val bookmarks = document.get("bookmark") as? List<String> ?: emptyList()
                        if (bookmarks.contains(boothId)) {
                            // 북마크가 되어 있는 경우, 아이콘을 @drawable/ic_flower로 변경
                            binding.icBookmark.setImageResource(R.drawable.ic_flower)
                        } else {
                            // 북마크가 되어 있지 않은 경우, 아이콘을 @drawable/ic_yellow_flower로 변경
                            binding.icBookmark.setImageResource(R.drawable.ic_yellow_flower)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("DetailActivity", "Failed to check bookmark: ${e.message}")
                }
        }
    }

    private fun toggleBookmarkStatus() {
        val user = FirebaseAuthManager.auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("user").document(user.uid)

            userRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val bookmarks = document.get("bookmark") as? List<String> ?: emptyList()

                        if (bookmarks.contains(boothId)) {
                            // 북마크가 되어 있으면 삭제
                            userRef.update("bookmark", FieldValue.arrayRemove(boothId))
                                .addOnSuccessListener {
                                    Toast.makeText(this, "북마크가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                    binding.icBookmark.setImageResource(R.drawable.ic_yellow_flower)
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "북마크 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            // 북마크가 되어 있지 않으면 추가
                            userRef.update("bookmark", FieldValue.arrayUnion(boothId))
                                .addOnSuccessListener {
                                    Toast.makeText(this, "북마크가 추가되었습니다.", Toast.LENGTH_SHORT).show()
                                    binding.icBookmark.setImageResource(R.drawable.ic_flower)
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "북마크 추가 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("DetailActivity", "Failed to toggle bookmark: ${e.message}")
                }
        }
    }

    override fun onResume() {
        super.onResume()
        checkBookmarkStatus()  // 북마크 상태 새로 고침
        findBooth()  // 부스 정보 새로 고침
    }
}