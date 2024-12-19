package com.example.bloombooth

import android.util.Log

class ReviewEditImageAdapter(
    override val images: MutableList<String>,
    private val deletedImages: MutableList<String>
) : ImageAdapter(images, { position -> }, { imageUrl -> }) {

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
            notifyItemRangeChanged(position, images.size)
        }
    }
}
