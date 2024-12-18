package com.example.bloombooth

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bloombooth.databinding.ItemMypageReviewBinding

class MypageReviewAdapter(private val context: Context, private val reviewList: List<ReviewItem>) : RecyclerView.Adapter<MypageReviewAdapter.ReviewViewHolder>() {

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
            binding.boothCount.text = "부스 수: ${review.booth_cnt}"
            binding.accessoryCondition.text = "액세서리 상태: ${review.accs_condi}"
            binding.accessoryCount.text = "액세서리 수: ${review.accs_cnt}"
            binding.retouchingStatus.text = "리터칭 상태: ${review.retouching}"
            binding.reviewUsername.text = review.user_name
            binding.reviewText.text = review.review_text
            binding.ratingBar.rating = review.review_rating.toFloat()
            val photoAdapter = PhotoAdapter(review.photo_urls)
            binding.recyclerViewReviews.adapter = photoAdapter
        }
    }

    class PhotoAdapter(private val photoUrls: List<String>) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image_thumbnail, parent, false)
            return PhotoViewHolder(view)
        }

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            holder.bind(photoUrls[position])
        }

        override fun getItemCount(): Int = photoUrls.size

        inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            private val imageView: ImageView = itemView.findViewById(R.id.image_thumbnail)

            fun bind(imageUrl: String) {
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .into(imageView)
            }
        }
    }
}
