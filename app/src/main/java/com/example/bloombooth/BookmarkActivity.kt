package com.example.bloombooth

import Booth
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bloombooth.auth.FirebaseAuthManager
import com.example.bloombooth.databinding.ActivityBookmarkBinding
import com.example.bloombooth.databinding.ActivitySearchResultBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class BookmarkActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBookmarkBinding
    private lateinit var adapter: MypageBookmarkAdapter

    // 북마크 리스트
    private val bookmarkList = mutableListOf<Booth>()

    // DB
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookmarkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 뒤로 가기
        binding.header.btnBack.setOnClickListener {
            finish()
        }

        // 리사이클러뷰에서 아이템을 클릭했을 때
        adapter = MypageBookmarkAdapter { booth ->
            openDetailActivity(booth)
        }

        // 리사이클러뷰와 어댑터 연결
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // SwipeRefreshLayout 설정
        binding.swipeRefreshLayout.setOnRefreshListener {
            val userId = FirebaseAuthManager.auth.currentUser?.uid
            userId?.let {
                searchBookmark(it) // 스와이프 새로고침 시, 북마크 리스트를 새로 불러옴
            }
        }

        // 현재 사용자
        val user = FirebaseAuthManager.auth.currentUser
        if (user != null) {
            fetchUserData(user.uid)
        } else {
            binding.username.text = "사용자의 닉네임"
        }

        // 사용자 데이터 가져오기
        if (user != null) {
            searchBookmark(user.uid)
        }
    }

    // 사용자 데이터 가져오기
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

    // 북마크 ID 목록 가져오기
    private fun searchBookmark(userId: String) {
        db.collection("user").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val bookmarks = document.get("bookmark") as? List<String> ?: emptyList()

                if (bookmarks.isEmpty()) {
                    Toast.makeText(this, "북마크한 가게가 없습니다!", Toast.LENGTH_SHORT).show()
                    bookmarkList.clear()
                    updateUI(bookmarkList) // 북마크가 없을 때 UI를 업데이트
                    binding.swipeRefreshLayout.isRefreshing = false // 새로고침 완료
                } else {
                    searchBookmarkBooth(bookmarks)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "북마크 데이터를 가져오는 데 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.swipeRefreshLayout.isRefreshing = false // 새로고침 완료
            }
    }

    // 북마크된 부스 데이터 가져오기
    private fun searchBookmarkBooth(bookmarks: List<String>) {
        // 북마크 리스트를 새로 가져올 때마다 초기화
        bookmarkList.clear()

        for (bookmark in bookmarks) {
            db.collection("booth").document(bookmark)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val booth = document.toObject(Booth::class.java)
                        if (booth != null) {
                            booth.booth_id = document.id
                            bookmarkList.add(booth)
                        }
                    }
                    // 모든 부스를 가져온 후, 정렬하고 UI 업데이트
                    if (bookmarkList.size == bookmarks.size) {
                        sortBoothList() // 부스 리스트 정렬
                        updateUI(bookmarkList) // 정렬된 리스트로 UI 업데이트
                        binding.swipeRefreshLayout.isRefreshing = false // 새로고침 완료
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "부스 데이터를 가져오는 데 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.swipeRefreshLayout.isRefreshing = false // 새로고침 완료
                }
        }
    }

    // 부스 리스트 정렬 (review_avg 내림차순 -> booth_id 오름차순)
    private fun sortBoothList() {
        bookmarkList.sortWith { booth1, booth2 ->
            // review_avg 내림차순으로 비교
            val reviewAvgComparison = booth2.review_avg.compareTo(booth1.review_avg)

            // review_avg가 같을 경우 booth_id 오름차순으로 비교
            if (reviewAvgComparison == 0) {
                booth1.booth_id?.compareTo(booth2.booth_id ?: "") ?: 0
            } else {
                reviewAvgComparison
            }
        }

    }

    private fun updateUI(boothList: List<Booth>) {
        adapter.submitList(boothList)
    }

    private fun openDetailActivity(booth: Booth) {
        val intent = Intent(this, DetailActivity::class.java)
        intent.putExtra("boothNumber", booth.booth_number)
        intent.putExtra("boothName", booth.booth_name)
        intent.putExtra("boothId", booth.booth_id)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
    }
}