package com.example.bloombooth

import android.util.Log

class ReviewEditImageAdapter(
    images: MutableList<String>,
    private val deletedImages: MutableList<String>,
) : ImageAdapter(images, { position -> }, { imageUrl -> }) {

    fun addNewImage(newImageUrl: String) {
        addImage(newImageUrl)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        holder.binding.deleteButton.setOnClickListener {
            val imagePath = images[position]
            if (imagePath.startsWith("http")) {
                deletedImages.add(imagePath)
            }
            images.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
