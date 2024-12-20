package com.example.bloombooth

import android.content.Context
import android.content.Intent
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

class MypageReviewAdapter(private val context: Context, val reviewList: MutableList<ReviewItem>) : RecyclerView.Adapter<MypageReviewAdapter.ReviewViewHolder>() {
    private val db = FirebaseFirestore.getInstance()

    inner class ReviewViewHolder(private val binding: ItemMypageReviewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(review: ReviewItem) {
            binding.boothName.text = review.booth_name

            binding.userName.text = review.user_name
            binding.reviewRating.text = review.review_rating.toString()
            binding.reviewDate.text = review.review_date
            binding.reviewText.text = review.review_text

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
                    putExtra("review_id", review.review_id)
                    putExtra("review_text", review.review_text)
                    putExtra("review_rating", review.review_rating)
                    putExtra("booth_id", review.booth_id)
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

    private fun deleteReview(review: ReviewItem) {
        val reviewId = review.review_id
        val userId = FirebaseAuthManager.auth.currentUser?.uid
        if (userId != null) {
            db.collection("review").document(reviewId)
                .delete()
                .addOnSuccessListener {
                    val userRef = db.collection("user").document(userId)
                    userRef.update("review_ids", FieldValue.arrayRemove(reviewId))
                        .addOnSuccessListener {
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