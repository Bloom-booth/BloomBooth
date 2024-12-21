package com.example.bloombooth

import Booth
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bloombooth.auth.FirebaseAuthManager
import com.example.bloombooth.databinding.ItemUserBookmarkBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MypageBookmarkAdapter(
    private val onItemClick: (Booth) -> Unit
) : RecyclerView.Adapter<MypageBookmarkAdapter.BookmarkViewHolder>() {

    private var bookmarkList: List<Booth> = listOf()

    // ViewHolder 정의
    inner class BookmarkViewHolder(val binding: ItemUserBookmarkBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(booth: Booth) {
            // 부스 이름
            binding.boothName.text = booth.booth_name

            // 평점 및 리뷰 수
            binding.ratingAvg.text = booth.review_avg.toString() // 평점 (숫자)
            binding.ratingStar.rating = booth.review_avg.toFloat() // 평점 (별)
            binding.reviewCnt.text = booth.review_cnt.toString() // 리뷰 수

            // 북마크 아이콘 클릭 리스너
            binding.icBookmark.setOnClickListener {
                booth.booth_id?.let { it1 -> toggleBookmarkStatus(it1, binding) }
            }

            // 부스 이름을 클릭하면
            binding.boothArea.setOnClickListener {
                onItemClick(booth)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val binding = ItemUserBookmarkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookmarkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        val booth = bookmarkList[position]
        holder.bind(booth)
    }

    override fun getItemCount(): Int {
        return bookmarkList.size
    }

    fun submitList(list: List<Booth>) {
        bookmarkList = list
        notifyDataSetChanged()
    }

    // ViewHolder의 binding을 통해 아이콘 상태를 업데이트
    private fun toggleBookmarkStatus(boothId: String, binding: ItemUserBookmarkBinding) {
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
                                    binding.icBookmark.setImageResource(R.drawable.ic_yellow_flower)
                                }
                                .addOnFailureListener { e ->
                                    Log.e("MypageBookmarkAdapter", "Failed to remove bookmark: ${e.message}")
                                }
                        } else {
                            // 북마크가 되어 있지 않으면 추가
                            userRef.update("bookmark", FieldValue.arrayUnion(boothId))
                                .addOnSuccessListener {
                                    binding.icBookmark.setImageResource(R.drawable.ic_flower)
                                }
                                .addOnFailureListener { e ->
                                    Log.e("MypageBookmarkAdapter", "Failed to add bookmark: ${e.message}")
                                }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MypageBookmarkAdapter", "Failed to toggle bookmark: ${e.message}")
                }
        }
    }
}