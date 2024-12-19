package com.example.bloombooth

import android.util.Log

class ReviewEditImageAdapter(
    images: MutableList<String>,
    private val deletedImages: MutableList<String>,
) : ImageAdapter(images, { position -> }, { imageUrl -> }) {

    // 이미지 추가 함수 호출을 위한 메서드
    fun addNewImage(newImageUrl: String) {
        addImage(newImageUrl)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        holder.binding.deleteButton.setOnClickListener {
            val imagePath = images[position]

            if (imagePath.startsWith("http")) {
                deletedImages.add(imagePath)
                Log.d("YEONJAE", "삭제 리스트에 이미지 추가 완료! 이미지 URL: $imagePath")
            }
            images.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
