package com.example.bloombooth

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bloombooth.auth.FirebaseAuthManager
import com.example.bloombooth.databinding.ActivityBookmarkBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class BookmarkActivity : AppCompatActivity() {
    val binding: ActivityBookmarkBinding by lazy { ActivityBookmarkBinding.inflate(layoutInflater) }
    private val db = FirebaseFirestore.getInstance()
    private lateinit var bookmarkAdapter: MypageBookmarkAdapter
    private val bookmarkList = mutableListOf<BookmarkItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        val user = FirebaseAuthManager.auth.currentUser
        if (user != null) {
            fetchUserData(user.uid)
        } else {
            binding.username.text = "사용자의 닉네임"
        }

        val userId = FirebaseAuthManager.auth.currentUser?.uid ?: return
        binding.bookmarkBackBtn.setOnClickListener {
            finish()
        }

        bookmarkAdapter = MypageBookmarkAdapter(bookmarkList, { position, isBookmarked ->
            val item = bookmarkList[position]
            if (isBookmarked) {
                addBookmarkToDb(item)
            } else {
                val dialog = AlertDialog.Builder(this)
                    .setTitle("북마크 삭제")
                    .setMessage("정말로 이 북마크를 삭제하시겠습니까?")
                    .setPositiveButton("예") { _, _ ->
                        removeBookmarkFromDb(item, position)
                    }
                    .setNegativeButton("아니오", null)
                    .create()
                dialog.show()
            }
            item.isBookmarked = isBookmarked
            bookmarkAdapter.notifyItemChanged(position)
        }, db, userId)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = bookmarkAdapter
    }

    private fun fetchUserData(userId: String) {
        db.collection("user").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nickname = document.getString("nickname") ?: "사용자의 닉네임"
                    binding.username.text = nickname
                    fetchBookmarks(userId)
                } else {
                    Toast.makeText(this, "사용자 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "데이터를 가져오는 데 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchBookmarks(userId: String) {
        db.collection("user").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val bookmarks = document.get("bookmark") as List<Map<String, String>>
                bookmarkList.clear()

                for (bookmark in bookmarks) {
                    val id = bookmark["id"] ?: ""
                    val name = bookmark["name"] ?: ""
                    val isBookmarked = true
                    bookmarkList.add(BookmarkItem(id, name, isBookmarked))
                }

                bookmarkAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "북마크 데이터를 가져오는 데 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addBookmarkToDb(item: BookmarkItem) {
        val userId = FirebaseAuthManager.auth.currentUser?.uid ?: return
        val bookmarkData = hashMapOf(
            "id" to item.id,
            "name" to item.name
        )

        db.collection("user").document(userId)
            .update("bookmark", FieldValue.arrayUnion(bookmarkData))
            .addOnSuccessListener {
                Toast.makeText(this, "북마크에 추가되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "북마크 추가 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeBookmarkFromDb(item: BookmarkItem, position: Int) {
        val userId = FirebaseAuthManager.auth.currentUser?.uid ?: return
        val bookmarkData = hashMapOf(
            "id" to item.id,
            "name" to item.name
        )

        db.collection("user").document(userId)
            .update("bookmark", FieldValue.arrayRemove(bookmarkData))
            .addOnSuccessListener {
                bookmarkList.removeAt(position)
                bookmarkAdapter.notifyItemRemoved(position)
                Toast.makeText(this, "북마크가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "북마크 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
