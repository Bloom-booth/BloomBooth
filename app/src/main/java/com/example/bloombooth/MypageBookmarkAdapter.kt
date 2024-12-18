package com.example.bloombooth

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.bloombooth.databinding.ItemUserBookmarkBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MypageBookmarkAdapter(
    private val items: MutableList<BookmarkItem>,
    private val onHeartClick: (Int, Boolean) -> Unit,
    private val db: FirebaseFirestore,
    private val userId: String
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
                    deleteBookmarkFromDatabase(item.id, position, holder.itemView.context)
                }
                .setNegativeButton("아니오", null)
                .create()

            dialog.show()
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    private fun deleteBookmarkFromDatabase(bookmarkId: String, position: Int, context: Context) {
        val userRef = db.collection("user").document(userId)
        val bookmarkToDelete = mapOf(
            "id" to bookmarkId,
            "name" to items[position].name
        )

        userRef.update("bookmark", FieldValue.arrayRemove(bookmarkToDelete))
            .addOnSuccessListener {
                Log.d("Bookmark", "Bookmark deleted successfully")

                items.removeAt(position)
                notifyItemRemoved(position)

                Toast.makeText(context, "북마크가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.w("Bookmark", "Error deleting bookmark", e)
                Toast.makeText(context, "북마크 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
