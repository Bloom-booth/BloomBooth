package com.example.bloombooth

import Booth
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bloombooth.auth.FirebaseAuthManager
import com.example.bloombooth.databinding.ItemMypageReviewBinding
import com.google.android.libraries.places.api.model.kotlin.review
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MypageReviewAdapter(
    private val context: Context,
    val reviewList: MutableList<Review>
) : RecyclerView.Adapter<MypageReviewAdapter.ReviewViewHolder>() {
    private val db = FirebaseFirestore.getInstance()

    inner class ReviewViewHolder(private val binding: ItemMypageReviewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(review: Review) {
            binding.boothName.text = review.booth_name

            binding.userName.text = review.user_name
            binding.reviewRating.text = review.review_rating.toString()
            binding.reviewDate.text = review.review_date

            binding.reviewText.apply {
                if (review.review_text.isNullOrEmpty()) {
                    visibility = View.GONE
                } else {
                    text = review.review_text
                    visibility = View.VISIBLE
                }
            }

            // 4개의 항목을 전부 입력하지 않았으면 사라지게 한다.
            if (review.booth_cnt == 0 && review.accs_condi == 0 && review.accs_cnt == 0 && review.retouching == 0) {
                binding.reviewFrame.visibility = View.GONE
            } else {
                binding.boothCnt.text = review.booth_cnt.toString()
                binding.retouching.text = goodOrBad(review.retouching)
                binding.accsCondi.text = goodOrBad(review.accs_condi)
                binding.accsCnt.text = lessOrLot(review.accs_cnt)
            }

            // 있는 사진만 띄운다.
            binding.pic1.visibility = View.GONE
            binding.pic2.visibility = View.GONE
            binding.pic3.visibility = View.GONE
            // 부스 사진 (Glide로 로드)
            if (review.photo_urls.isNotEmpty()) {
                // 1번 사진
                binding.pic1.visibility = View.VISIBLE
                Glide.with(binding.root.context)
                    .load(toSecureURL(review.photo_urls[0]))
                    .placeholder(R.drawable.ic_flower)
                    .error(R.drawable.ic_flower)
                    .into(binding.pic1)

                // 2번 사진
                if (review.photo_urls.size > 1) {
                    binding.pic2.visibility = View.VISIBLE
                    Glide.with(binding.root.context)
                        .load(toSecureURL(review.photo_urls[1]))
                        .placeholder(R.drawable.ic_flower)
                        .error(R.drawable.ic_flower)
                        .into(binding.pic2)
                }

                // 3번 사진
                if (review.photo_urls.size > 2) {
                    binding.pic3.visibility = View.VISIBLE
                    Glide.with(binding.root.context)
                        .load(toSecureURL(review.photo_urls[2]))
                        .placeholder(R.drawable.ic_flower)
                        .error(R.drawable.ic_flower)
                        .into(binding.pic3)
                }
            }

            // 수정하기
            binding.reviewEdit.setOnClickListener {
                val intent = Intent(context, ReviewEditActivity::class.java).apply {
                    putExtra("review_date", review.review_date)
                    putExtra("review_id", review.review_id)
                    putExtra("review_text", review.review_text)
                    putExtra("review_rating", review.review_rating)
                    putExtra("booth_id", review.booth_id)
                    putExtra("booth_name", review.booth_name)
                    putExtra("user_id", review.user_id)
                    putExtra("user_name", review.user_name)
                    putExtra("booth_cnt", review.booth_cnt)
                    putExtra("accs_cnt", review.accs_cnt)
                    putStringArrayListExtra("photo_urls", ArrayList(review.photo_urls))
                    putExtra("accs_condi",review.accs_condi)
                    putExtra("retouching", review.retouching)
                }
                context.startActivity(intent)
            }
            // 삭제하기
            binding.reviewDelete.setOnClickListener {
                deleteReview(review)
            }
        }
    }

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

    private fun deleteReview(review: Review) {
        val reviewId = review.review_id
        val userId = FirebaseAuthManager.auth.currentUser?.uid
        if (userId != null) {
            db.collection("review").document(reviewId)
                .delete()
                .addOnSuccessListener {
                    val userRef = db.collection("user").document(userId)
                    userRef.update("review_ids", FieldValue.arrayRemove(reviewId))
                        .addOnSuccessListener {
                            updateBoothReviewDelete(review)
                            reviewList.remove(review)
                            notifyDataSetChanged()

                            Toast.makeText(context, "리뷰가 성공적으로 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "리뷰 목록을 업데이트하는 데 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "리뷰 삭제에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateBoothReviewDelete(review: Review) {
        db.collection("booth").document(review.booth_id)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Firestore에서 Booth 객체를 가져오기
                    var updateBooth = document.toObject(Booth::class.java)

                    // updateBooth가 null인 경우 처리
                    if (updateBooth != null) {
                        // 기존 리뷰 점수와 카운트 값
                        val oldRating = updateBooth.review_avg ?: 0
                        val newRating = review.review_rating // 삭제된 리뷰의 점수
                        val oldReviewCnt = updateBooth.review_cnt ?: 0

                        // 새로운 review_avg와 review_cnt 계산
                        if (oldReviewCnt > 1) {
                            updateBooth.review_avg = ((oldRating * oldReviewCnt - newRating) / (oldReviewCnt - 1))
                            updateBooth.review_cnt = oldReviewCnt - 1
                        } else {
                            // 리뷰가 1개일 경우 삭제되면 0으로 설정
                            updateBooth.review_avg = 0
                            updateBooth.review_cnt = 0
                        }


                        // Firestore에서 review_avg와 review_cnt만 업데이트
                        db.collection("booth").document(review.booth_id)
                            .update(
                                "review_avg", updateBooth.review_avg,
                                "review_cnt", updateBooth.review_cnt
                            )
                            .addOnSuccessListener {
                                Log.d("Update Success", "Booth review updated successfully")
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Firestore Error", "Error updating booth review: ${exception.message}")
                            }
                    } else {
                        Log.e("Firestore Error", "Booth document not found")
                    }
                } else {
                    Log.e("Firestore Error", "Document does not exist")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore Error", "Error retrieving document: ${exception.message}")
            }
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

    private fun toSecureURL(vulnURL: String) : String {
        var secureURL: String = vulnURL
        if (vulnURL.startsWith("http://")) {
            secureURL = vulnURL.replace("http://", "https://")
        }
        return secureURL
    }
}