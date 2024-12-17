package com.example.bloombooth

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bloombooth.databinding.ItemUserBookmarkBinding

class MypageBookmarkAdapter(
    private val items: MutableList<BookmarkItem>,
    private val onHeartClick: (Int, Boolean) -> Unit
) : RecyclerView.Adapter<MypageBookmarkAdapter.BookmarkViewHolder>() {

    class BookmarkViewHolder(val binding: ItemUserBookmarkBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val binding = ItemUserBookmarkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookmarkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        val item = items[position]
        holder.binding.itemText.text = item.name

        val heartIcon = if (item.isBookmarked) {
            R.drawable.heart_filled
        } else {
            R.drawable.heart_empty
        }
        holder.binding.heartButton.setImageResource(heartIcon)

        holder.binding.heartButton.setOnClickListener {
            val dialog = AlertDialog.Builder(holder.itemView.context)
                .setTitle("북마크 삭제")
                .setMessage("정말로 이 북마크를 삭제하시겠습니까?")
                .setPositiveButton("예") { _, _ ->
                    items.removeAt(position)
                    notifyItemRemoved(position)
                }
                .setNegativeButton("아니오", null)
                .create()

            dialog.show()
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
