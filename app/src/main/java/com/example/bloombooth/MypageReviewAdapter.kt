package com.example.bloombooth

import android.content.Context
import android.content.Intent
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

            binding.reviewEditBtn.setOnClickListener {
                val intent = Intent(context, ReviewEditActivity::class.java).apply {
                    putExtra("review_id", review.review_id)
                    putExtra("review_text", review.review_text)
                    putExtra("review_rating", review.review_rating)
                    putExtra("review_date", review.review_date)
                    putExtra("booth_id", review.booth_id)
                    putExtra("booth_cnt", review.booth_cnt)
                    putExtra("accs_cnt", review.accs_cnt)
                    putStringArrayListExtra("photo_urls", ArrayList(review.photo_urls))
                    putExtra("accs_condi",review.accs_condi)
                    putExtra("retouching", review.retouching)
                }
                context.startActivity(intent)
            }
            val photoAdapter = PhotoAdapter(review.photo_urls)
            binding.reviewImages.adapter = photoAdapter
            binding.reviewImages.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
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
            private fun ensureHttps(url: String): String {
                return if (url.startsWith("http://")) {
                    url.replaceFirst("http://", "https://")
                } else {
                    url
                }
            }

            fun bind(imageUrl: String) {
                val secureImageUrl = ensureHttps(imageUrl)
                Glide.with(itemView.context)
                    .load(secureImageUrl)
                    .error(R.drawable.error_image)
                    .into(imageView)
            }
        }
    }
}
