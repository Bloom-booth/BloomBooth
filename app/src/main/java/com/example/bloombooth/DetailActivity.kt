package com.example.bloombooth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.bloombooth.auth.FirebaseAuthManager
import com.example.bloombooth.databinding.ActivityDetailBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class DetailActivity : AppCompatActivity() {
    val binding : ActivityDetailBinding by lazy {ActivityDetailBinding.inflate(layoutInflater)}
    private val db = FirebaseFirestore.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        val boothId = "장소 id : Z3KFDK ~ "
        val boothName = "장소 이름 : ex) 포토이즘 숙명여대점"

        // 북마크 추가하는 방법
        val bookmarkData = hashMapOf(
            "id" to boothId,
            "name" to boothName,
            "isBookmarked" to true
        )

        val user = FirebaseAuthManager.auth.currentUser
        if (user != null) {
            db.collection("user")
                .document(user.uid)
                .update("bookmark", FieldValue.arrayUnion(bookmarkData))
                .addOnSuccessListener {
                    Toast.makeText(this, "북마크가 추가되었습니다.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "북마크 추가 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // 리뷰 작성 페이지 갈 때 넘길 인텐트 정보들
        binding.writeReviewBtn.setOnClickListener {
            val intent = Intent(this, ReviewActivity::class.java)
            intent.putExtra("boothName", boothName)
            intent.putExtra("boothId", boothId)
            startActivity(intent)
        }
    }
}