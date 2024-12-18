package com.example.bloombooth

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bloombooth.auth.FirebaseAuthManager
import com.example.bloombooth.databinding.ItemMypageReviewBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MypageReviewAdapter(private val context: Context, private val reviewList: MutableList<ReviewItem>) : RecyclerView.Adapter<MypageReviewAdapter.ReviewViewHolder>() {
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemMypageReviewBinding.inflate(LayoutInflater.from(context), parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviewList[position]
        holder.bind(review)
    }

    override fun getItemCount(): Int {
        return reviewList.size
    }

    inner class ReviewViewHolder(private val binding: ItemMypageReviewBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(review: ReviewItem) {
            binding.reviewDate.text = review.review_date

            var tmp = review.booth_cnt.toString()
            if (tmp.equals("0")) tmp = "?"
            binding.boothCount.text = tmp
            binding.accessoryCondition.text = changeValueToWordStatus(review.accs_condi)
            binding.accessoryCount.text = changeValueToWordCnt(review.accs_cnt)
            binding.retouchingStatus.text = changeValueToWordStatus(review.retouching)
            binding.reviewUsername.text = review.user_name
            binding.reviewText.text = review.review_text
            binding.ratingBar.text = review.review_rating.toString()
            binding.boothName.text = review.booth_name

            binding.reviewDeleteBtn.setOnClickListener {
                deleteReview(review)
            }

            val photoAdapter = PhotoAdapter(review.photo_urls)
            binding.reviewImages.adapter = photoAdapter
            binding.reviewImages.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        private fun deleteReview(review: ReviewItem) {
            val reviewId = review.review_id // review_id 사용
            val userId = FirebaseAuthManager.auth.currentUser?.uid
            if (userId != null) {
                // Firestore에서 해당 리뷰 삭제
                db.collection("review").document(reviewId)
                    .delete()
                    .addOnSuccessListener {
                        // Firestore에서 사용자의 리뷰 목록 업데이트
                        val userRef = db.collection("user").document(userId)
                        userRef.update("review_ids", FieldValue.arrayRemove(reviewId))
                            .addOnSuccessListener {
                                // 삭제 완료 후 데이터 새로고침
                                Toast.makeText(context, "리뷰가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                fetchReviews()  // 전체 리뷰 목록을 다시 가져오는 함수 호출
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "사용자의 리뷰 목록을 업데이트하는 데 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        // 리뷰 삭제 실패 시 메시지
                        Toast.makeText(context, "리뷰 삭제에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        private fun fetchReviews() {
            // 리뷰 데이터를 Firestore에서 새로 가져오는 로직
            db.collection("review")
                .get()
                .addOnSuccessListener { documents ->
                    val updatedReviews = mutableListOf<ReviewItem>()
                    for (document in documents) {
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

                        // booth_id에 해당하는 booth_name을 가져오는 Firestore 쿼리 추가
                        if (boothId.isNotEmpty()) {
                            db.collection("booth").document(boothId)
                                .get()
                                .addOnSuccessListener { boothDoc ->
                                    if (boothDoc.exists()) {
                                        val boothName = boothDoc.getString("booth_name") ?: "Booth 이름 없음"
                                        // ReviewItem 객체로 데이터를 변환
                                        val reviewItem = ReviewItem(
                                            review_date = reviewDateString,
                                            booth_cnt = boothCnt,
                                            accs_condi = accsCondi,
                                            accs_cnt = accsCnt,
                                            retouching = retouching,
                                            review_rating = reviewRating,
                                            booth_id = boothId,
                                            user_id = userId,
                                            user_name = userName,
                                            review_text = reviewText,
                                            photo_urls = photoUrls,
                                            booth_name = boothName
                                        )
                                        // 업데이트된 리뷰 데이터를 리스트에 추가
                                        updatedReviews.add(reviewItem)

                                        // UI 업데이트: 전체 리뷰 목록을 업데이트
                                        updateUIWithReviews(updatedReviews)
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("FetchBooth", "Error fetching booth data: ${e.message}")
                                }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "리뷰 데이터를 가져오는 데 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        private fun updateUIWithReviews(reviews: List<ReviewItem>) {
            // 어댑터에 새 데이터를 업데이트
            reviewList.clear()
            reviewList.addAll(reviews)
            notifyDataSetChanged() // 전체 어댑터에 데이터가 변경되었음을 알림
        }

        fun changeValueToWordStatus(i: Int): String {
            return when (i) {
                1 -> "없음"
                2 -> "나쁨"
                3 -> "보통"
                4 -> "좋음"
                else -> "?"
            }
        }

        fun changeValueToWordCnt(i: Int): String {
            return when (i) {
                1 -> "없음"
                2 -> "적음"
                3 -> "보통"
                4 -> "많음"
                else -> "?"
            }
        }
    }

    class PhotoAdapter(private val photoUrls: List<String>) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_simple_images, parent, false)
            return PhotoViewHolder(view)
        }

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            val imageUrl = photoUrls[position]
            Log.d("BindCheck", "Binding item at position: $position with URL: $imageUrl")
            holder.bind(imageUrl)
        }

        override fun getItemCount(): Int = photoUrls.size

        inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            private val imageView: ImageView = itemView.findViewById(R.id.image_thumbnail)

            fun bind(imageUrl: String) {
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .error(R.drawable.error_image)
                    .into(imageView)
            }
        }
    }
}
