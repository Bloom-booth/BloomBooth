package com.example.bloombooth

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bloombooth.databinding.ItemImageThumbnailBinding

class ImageAdapter(
    private val imageList: MutableList<String>,
    private val onDelete: (Int) -> Unit,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(private val binding: ItemImageThumbnailBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(uri: String) {
            binding.imageThumbnail.setImageURI(Uri.parse(uri))
            binding.deleteButton.setOnClickListener {
                onDelete(adapterPosition)
            }
            binding.root.setOnClickListener {
                onClick(uri)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageThumbnailBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(imageList[position])
    }

    override fun getItemCount(): Int {
        return imageList.size
    }
}
