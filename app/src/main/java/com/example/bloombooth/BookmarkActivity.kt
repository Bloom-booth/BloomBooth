package com.example.bloombooth

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bloombooth.auth.FirebaseAuthManager.auth
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

        val user = auth.currentUser
        if (user != null) {
            fetchUserData(user.uid)
        } else {
            binding.username.text = "사용자의 닉네임"
        }

        val newBookmark = mapOf(
            "id" to "TJhSkTVYOR3Zn8Dd85sw",
            "name" to "인생네컷 숙명여대점"
        )

        val userId = user?.uid
        if (userId != null) {
            db.collection("user").document(userId)
                .update("bookmark", FieldValue.arrayUnion(newBookmark))
                .addOnSuccessListener {
                    Toast.makeText(this, "북마크가 추가되었습니다.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "북마크 추가 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        binding.bookmarkBackBtn.setOnClickListener {
            finish()
        }

        bookmarkAdapter = MypageBookmarkAdapter(bookmarkList) { position, isBookmarked ->
            val item = bookmarkList[position]
            if (isBookmarked) {
                addBookmarkToDb(item)
            } else {
                removeBookmarkFromDb(item)
            }
            item.isBookmarked = isBookmarked
            bookmarkAdapter.notifyItemChanged(position)
        }

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
                    Toast.makeText(this, "사용자 데이터를 찾을 수 없습니다.",
                        Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "데이터를 가져오는 데 실패했습니다: ${e.message}",
                    Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "북마크 데이터를 가져오는 데 실패했습니다: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun addBookmarkToDb(item: BookmarkItem) {
        val userId = auth.currentUser?.uid ?: return
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

    private fun removeBookmarkFromDb(item: BookmarkItem) {
        val userId = auth.currentUser?.uid ?: return
        val bookmarkData = hashMapOf(
            "id" to item.id,
            "name" to item.name
        )

        db.collection("user").document(userId)
            .update("bookmark", FieldValue.arrayRemove(bookmarkData))
            .addOnSuccessListener {
                Toast.makeText(this, "북마크가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "북마크 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

