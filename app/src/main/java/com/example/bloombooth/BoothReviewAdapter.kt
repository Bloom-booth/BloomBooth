package com.example.bloombooth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bloombooth.databinding.ItemDetailBinding

class BoothReviewAdapter : RecyclerView.Adapter<BoothReviewAdapter.ReviewViewHolder>() {

    private var reviewList: List<Review> = listOf()

    // ViewHolder 정의
    inner class ReviewViewHolder(private val binding: ItemDetailBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(review: Review) {
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
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviewList[position]
        holder.bind(review)
    }

    override fun getItemCount(): Int {
        return reviewList.size
    }

    fun submitList(list: List<Review>) {
        reviewList = list
        notifyDataSetChanged()
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