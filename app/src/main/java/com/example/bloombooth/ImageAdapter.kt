package com.example.bloombooth

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.bloombooth.databinding.ItemImageThumbnailBinding

open class ImageAdapter(
    open val images: MutableList<String>,
    private val onDeleteClick: (Int) -> Unit,
    private val onImageClick: (String) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {
    open fun addImage(newImageUrl: String) {
        images.add(newImageUrl)
        notifyItemInserted(images.size - 1)
    }


    inner class ImageViewHolder(val binding: ItemImageThumbnailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(imagePath: String, position: Int) {
            val secureImageUrl = if (imagePath.startsWith("http://")) {
                imagePath.replace("http://", "https://")
            } else {
                imagePath
            }

            Glide.with(binding.root.context)
                .load(secureImageUrl)
                .placeholder(R.drawable.loading_image)
                .error(R.drawable.error_image)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.imageThumbnail)

            binding.deleteButton.setOnClickListener {
                it.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    onDeleteClick(position)
                }.start()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageThumbnailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position], position)
    }

    override fun getItemCount(): Int = images.size
}