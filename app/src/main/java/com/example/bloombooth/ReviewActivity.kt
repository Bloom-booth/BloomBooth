package com.example.bloombooth

import Booth
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bloombooth.auth.FirebaseAuthManager
import com.example.bloombooth.databinding.ActivityReviewBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.util.Date
import com.cloudinary.Cloudinary
import kotlinx.coroutines.*
import com.cloudinary.utils.ObjectUtils
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class ReviewActivity : AppCompatActivity() {
    private val binding: ActivityReviewBinding by lazy { ActivityReviewBinding.inflate(layoutInflater) }

    private val imageList = mutableListOf<String>()

    private val imagePickerResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                // Ensure the image list size is within limits
                if (imageList.size < 3) {
                    imageList.add(imageUri.toString()) // Add the URI as a string
                    updateImagePreviews()  // Update UI with the newly selected image
                } else {
                    showToast("이미지는 최대 3개까지 업로드할 수 있습니다.")
                }
            } else {
                showToast("이미지를 선택할 수 없습니다.")
            }
        } else {
            showToast("이미지 선택을 취소했습니다.")
        }
    }

    private val db = FirebaseFirestore.getInstance()

    private var userId: String = ""
    private var userName: String = ""

    private var boothId: String = ""
    private var boothName: String = ""

    private var boothCnt = 0
    private var accsCnt = 0
    private var accsCondi = 0
    private var retouching = 0
    private var reviewDate = DateFormatter.format(Date())
    private var reviewRating: Int = 0
    private var reviewText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // user 정보 받아오기
        userId = FirebaseAuthManager.auth.currentUser?.uid.toString()
        db.collection("user").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    userName = document.getString("nickname") ?: "Unknown"
                }
            }

        // booth 정보 받아오기
        boothId = intent.getStringExtra("boothId").toString()
        boothName = intent.getStringExtra("boothName").toString()

        // 뒤로가기
        binding.icBack.setOnClickListener{ finish() }

        // boothName 적용하기
        binding.boothName.text = boothName

        // boothCnt 드롭다운
        val boothCntValues = (1 .. 10).toList()
        val boothCntAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, boothCntValues)
        boothCntAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.boothCnt.adapter = boothCntAdapter

        // accsCnt, accsCondi, retouching 한글에서 숫자로
        selectedToInt(
            listOf(binding.accsCnt1, binding.accsCnt2, binding.accsCnt3, binding.accsCnt4)
        ) { selectedIndex ->
            accsCnt = selectedIndex
        }
        selectedToInt(
            listOf(binding.accsCondi1, binding.accsCondi2, binding.accsCondi3, binding.accsCondi4)
        ) { selectedIndex ->
            accsCondi = selectedIndex
        }
        selectedToInt(
            listOf(binding.retouching1, binding.retouching2, binding.retouching3, binding.retouching4)
        ) { selectedIndex ->
            retouching = selectedIndex
        }

        // 사진이 선택된다면 나타날 자리
        binding.pic1.visibility = View.GONE
        binding.pic2.visibility = View.GONE
        binding.pic3.visibility = View.GONE

        // 사진 선택하기 버튼
        binding.btnSelectImage.setOnClickListener {
            if (checkAndRequestPermission()) {
                openGallery()
            }
        }

        // 리뷰 등록하기 버튼 클릭 리스너
        binding.btnUploadReview.setOnClickListener {
            if (!validateRating()) {
                Toast.makeText(this, "별점을 선택해야 합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                saveReview()
            }
        }
    }

    private fun saveReview() {
        val db = Firebase.firestore
        val userRef = db.collection("user").document(userId)
        userRef.get().addOnSuccessListener { _ ->
            uploadImagesToCloudinary(imageList) { urls ->
                val reviewData = hashMapOf(
                    "review_date" to reviewDate,
                    "booth_cnt" to boothCnt,
                    "accs_condi" to accsCondi,
                    "accs_cnt" to accsCnt,
                    "retouching" to retouching,
                    "review_text" to binding.reviewText.text.toString(),
                    "review_rating" to reviewRating,
                    "booth_id" to boothId,
                    "user_id" to userId,
                    "user_name" to userName,
                    "photo_urls" to urls
                )

                db.collection("review")
                    .add(reviewData)
                    .addOnSuccessListener { documentReference ->
                        val reviewId = documentReference.id
                        db.collection("user").document(userId)
                            .update("review_ids", FieldValue.arrayUnion(reviewId))
                            .addOnSuccessListener {
                                showToast("리뷰가 성공적으로 등록되었습니다.")
                                updateBoothReview()
                                finish()
                            }
                            .addOnFailureListener {
                                showToast("리뷰 등록에 실패했습니다. 다시 시도해주세요.")
                            }
                    }
                    .addOnFailureListener {
                        showToast("리뷰 등록에 실패했습니다. 다시 시도해주세요.")
                    }
            }
        }.addOnFailureListener {
            showToast("사용자 정보를 불러오는 데 실패했습니다.")
        }
    }

    // 선택된 사진을 UI에 표시
    private fun updateImagePreviews() {
        binding.pic1.visibility = View.GONE
        binding.pic2.visibility = View.GONE
        binding.pic3.visibility = View.GONE

        imageList.forEachIndexed { index, uriString ->
            val uri = Uri.parse(uriString)
            when (index) {
                0 -> {
                    binding.pic1.setImageURI(uri)
                    binding.pic1.visibility = View.VISIBLE
                }
                1 -> {
                    binding.pic2.setImageURI(uri)
                    binding.pic2.visibility = View.VISIBLE
                }
                2 -> {
                    binding.pic3.setImageURI(uri)
                    binding.pic3.visibility = View.VISIBLE
                }
            }
        }
    }

    // Cloudinary 업로드
    private fun uploadImagesToCloudinary(
        imageUris: List<String>,
        onComplete: (List<String>) -> Unit
    ) {
        val cloudinary = Cloudinary(
            ObjectUtils.asMap(
                "cloud_name", getString(R.string.cloud_name),
                "api_key", getString(R.string.image_api_key),
                "api_secret", getString(R.string.image_api_secret)
            )
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val urls = imageUris.map { imageUri ->
                    val inputStream = contentResolver.openInputStream(Uri.parse(imageUri))
                    val fileName = "images/${UUID.randomUUID()}.jpg"

                    inputStream?.use {
                        val options = ObjectUtils.asMap(
                            "public_id", fileName,
                            "folder", "bloombooth"
                        )
                        val result = cloudinary.uploader().upload(it, options)
                        result["url"].toString()
                    } ?: throw Exception("Invalid input stream for URI: $imageUri")
                }

                withContext(Dispatchers.Main) {
                    onComplete(urls)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("이미지 업로드 중 오류가 발생했습니다: ${e.message}")
                }
            }
        }
    }

    private fun validateRating(): Boolean {
        reviewRating = binding.reviewRating.rating.toInt()
        return reviewRating > 0
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // 이미지 권한 체크
    private fun checkAndRequestPermission(): Boolean {
        val isPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        if (!isPermissionGranted) {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)
        }

        return isPermissionGranted
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        imagePickerResult.launch(intent)
    }

    // 한글에서 숫자로
    private fun selectedToInt(
        textViews: List<TextView>,
        onSelect: (Int) -> Unit
    ) {
        var lastSelectedIndex: Int? = null // 마지막으로 선택된 항목 저장

        textViews.forEachIndexed { index, textView ->
            textView.setOnClickListener {
                if (lastSelectedIndex == index) {
                    // 이미 선택된 것을 다시 클릭한 경우 선택 해제
                    textViews[index].setBackgroundResource(R.drawable.bg_light_pink) // 기본 배경
                    textViews[index].setTextColor(resources.getColor(R.color.black, null)) // 기본 텍스트 색
                    lastSelectedIndex = null // 선택 해제
                    onSelect(0) // 값 초기화
                } else {
                    // 다른 항목 선택
                    textViews.forEach {
                        it.setBackgroundResource(R.drawable.bg_light_pink) // 기본 배경
                        it.setTextColor(resources.getColor(R.color.black, null)) // 기본 텍스트 색
                    }
                    textView.setBackgroundResource(R.drawable.bg_dark_pink) // 선택된 배경
                    textView.setTextColor(resources.getColor(R.color.white, null)) // 선택된 텍스트 색
                    lastSelectedIndex = index // 현재 선택 항목 업데이트
                    onSelect(index + 1) // 선택된 값 전달
                }
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    private fun updateBoothReview() {
        db.collection("booth").document(boothId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Firestore에서 Booth 객체를 가져오기
                    var updateBooth = document.toObject(Booth::class.java)

                    // updateBooth가 null인 경우 처리
                    if (updateBooth != null) {
                        // 기존 리뷰 점수와 카운트 값
                        val oldRating = updateBooth.review_avg ?: 0
                        val newRating = reviewRating
                        val oldReviewCnt = updateBooth.review_cnt ?: 0

                        // 새로운 review_avg와 review_cnt 계산
                        updateBooth.review_avg = ((oldRating * oldReviewCnt + newRating) / (oldReviewCnt + 1))
                        updateBooth.review_cnt = oldReviewCnt + 1

                        // Firestore에서 review_avg와 review_cnt만 업데이트
                        db.collection("booth").document(boothId)
                            .update(
                                "review_avg", updateBooth.review_avg,
                                "review_cnt", updateBooth.review_cnt
                            )
                            .addOnSuccessListener {
                                Log.d("Update Success", "Booth review updated successfully")
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Firestore Error", "Error updating booth review: ${exception.message}")
                            }
                    } else {
                        Log.e("Firestore Error", "Booth document not found")
                    }
                } else {
                    Log.e("Firestore Error", "Document does not exist")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore Error", "Error retrieving document: ${exception.message}")
            }
    }

}