package com.example.bloombooth

import Booth
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bloombooth.databinding.ItemBoothBinding

class SearchResultAdapter(
    private val onItemClick: (Booth) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.BoothViewHolder>() {

    private var boothList: List<Booth> = listOf()

    // ViewHolder 정의
    inner class BoothViewHolder(private val binding: ItemBoothBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(booth: Booth) {
            // 부스 이름
            binding.boothName.text = booth.booth_name

            // 평점 및 리뷰 수
            binding.ratingAvg.text = booth.review_avg.toString() // 평점 (숫자)
            binding.ratingStar.rating = booth.review_avg.toFloat() // 평점 (별)
            binding.reviewCnt.text = booth.review_cnt.toString() // 리뷰 수

            // 부스 주소
            binding.boothAddr.text = booth.booth_addr

            // 부스 사진 (Glide로 로드)
            Glide.with(binding.root.context)
                .load(toSecureURL(booth.booth_pic))
                .placeholder(R.drawable.ic_flower)
                .error(R.drawable.ic_flower)
                .into(binding.boothPic)

            // 아이템을 클릭하면 booth 정보를 전달한다.
            binding.root.setOnClickListener {
                onItemClick(booth)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoothViewHolder {
        val binding = ItemBoothBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BoothViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BoothViewHolder, position: Int) {
        val booth = boothList[position]
        holder.bind(booth)
    }

    override fun getItemCount(): Int {
        return boothList.size
    }

    // 리스트 업데이트 메소드
    fun submitList(list: List<Booth>) {
        boothList = list
        notifyDataSetChanged()
    }

    private fun toSecureURL(vulnURL: String) : String {
        var secureURL: String = vulnURL
        if (vulnURL.startsWith("http://")) {
            secureURL = vulnURL.replace("http://", "https://")
        }
        return secureURL
    }
}