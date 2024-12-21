package com.example.bloombooth

import Booth
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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
import com.bumptech.glide.Glide
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.example.bloombooth.auth.FirebaseAuthManager
import com.example.bloombooth.databinding.ActivityReviewEditBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID

class ReviewEditActivity : AppCompatActivity() {
    private val binding: ActivityReviewEditBinding by lazy { ActivityReviewEditBinding.inflate(layoutInflater) }

    private val db = Firebase.firestore
    private var oldImageList = mutableListOf<String>()
    private var newImageList = mutableListOf<String>()
    private var deletedImageList = mutableListOf<String>()

    private lateinit var reviewText: String
    private lateinit var reviewId: String
    private lateinit var boothId: String
    private lateinit var boothName: String
    private lateinit var userId: String
    private lateinit var userName: String
    private var reviewRating = 0
    private var boothCnt = 0
    private var accsCnt = 0
    private var accsCondi = 0
    private var retouching = 0
    private var reviewDate: String = ""

    private val imagePickerResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { imageUri ->
                if (newImageList.size + oldImageList.filter { it.isNotEmpty() }.size < 3) {
                    newImageList.add(imageUri.toString())
                    updateImageViews()
                } else {
                    showToast("이미지는 최대 3개까지 업로드할 수 있습니다.")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        userId = intent.getStringExtra("user_id").toString()
        userName = intent.getStringExtra("user_name").toString()
        reviewDate = intent.getStringExtra("review_date").toString()
        reviewText = intent.getStringExtra("review_text").toString()
        reviewId = intent.getStringExtra("review_id") ?: return showErrorAndFinish("리뷰 ID를 확인할 수 없습니다.")
        boothId = intent.getStringExtra("booth_id") ?: return showErrorAndFinish("부스 ID를 확인할 수 없습니다.")
        boothName = intent.getStringExtra("booth_name").toString()
        reviewRating = intent.getIntExtra("review_rating", 0)
        boothCnt = intent.getIntExtra("booth_cnt", 0)
        accsCnt = intent.getIntExtra("accs_cnt", 0)
        accsCondi = intent.getIntExtra("accs_condi", 0)
        retouching = intent.getIntExtra("retouching", 0)
        oldImageList = intent.getStringArrayListExtra("photo_urls")?.toMutableList() ?: mutableListOf()

        binding.boothName.text = boothName
        binding.reviewText.setText(reviewText)
        binding.reviewRating.rating = reviewRating.toFloat()
        binding.boothCnt.setSelection((1..10).indexOf(boothCnt))

        initializeSelectionUI()
        initializeImages()

        // boothCnt 드롭다운
        val boothCntValues = (1..10).toList()
        val boothCntAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, boothCntValues)
        boothCntAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.boothCnt.adapter = boothCntAdapter

        // boothCnt 값을 Spinner에서 미리 선택
        val position = boothCntValues.indexOf(boothCnt)
        if (position >= 0) {
            binding.boothCnt.setSelection(position)
        }

        binding.icBack.setOnClickListener { showCancelDialog() }
        binding.btnSelectImage.setOnClickListener {
            if (checkAndRequestPermission()) openGallery()
        }
        binding.btnUploadReview.setOnClickListener {
            if (validateRating()) saveReview() else showToast("평점을 입력해 주세요.")
        }
    }

    private fun initializeSelectionUI() {
        preselect(listOf(binding.accsCnt1, binding.accsCnt2, binding.accsCnt3, binding.accsCnt4), accsCnt)
        preselect(listOf(binding.accsCondi1, binding.accsCondi2, binding.accsCondi3, binding.accsCondi4), accsCondi)
        preselect(listOf(binding.retouching1, binding.retouching2, binding.retouching3, binding.retouching4), retouching)

        addSelectionListener(listOf(binding.accsCnt1, binding.accsCnt2, binding.accsCnt3, binding.accsCnt4)) { accsCnt = it }
        addSelectionListener(listOf(binding.accsCondi1, binding.accsCondi2, binding.accsCondi3, binding.accsCondi4)) { accsCondi = it }
        addSelectionListener(listOf(binding.retouching1, binding.retouching2, binding.retouching3, binding.retouching4)) { retouching = it }
    }

    private fun initializeImages() = updateImageViews()

    private fun updateImageViews() {
        val allImages = oldImageList.filter { it.isNotEmpty() } + newImageList
        val imageViews = listOf(binding.pic1, binding.pic2, binding.pic3)
        val deleteButtons = listOf(binding.deletePic1, binding.deletePic2, binding.deletePic3)

        imageViews.forEachIndexed { index, imageView ->
            if (index < allImages.size) {
                Glide.with(this).load(toSecureURL(allImages[index])).into(imageView)
                imageView.visibility = View.VISIBLE
                deleteButtons[index].apply {
                    visibility = View.VISIBLE
                    setOnClickListener {
                        if (index < oldImageList.size) {
                            deletedImageList.add(oldImageList[index])
                            oldImageList[index] = ""
                        } else {
                            newImageList.removeAt(index - oldImageList.size)
                        }
                        updateImageViews()
                    }
                }
            } else {
                imageView.visibility = View.GONE
                deleteButtons[index].visibility = View.GONE
            }
        }
    }

    private fun toSecureURL(vulnURL: String) : String {
        var secureURL: String = vulnURL
        if (vulnURL.startsWith("http://")) {
            secureURL = vulnURL.replace("http://", "https://")
        }
        return secureURL
    }

    private fun validateRating() = binding.reviewRating.rating > 0

    private fun saveReview() {
        val userId = FirebaseAuthManager.auth.currentUser?.uid
            ?: return showErrorAndFinish("사용자 인증 정보를 확인할 수 없습니다.")
        val reviewText = binding.reviewText.text.toString()
        val reviewRating = binding.reviewRating.rating.toInt()

        // 새 이미지를 Cloudinary에 업로드
        val imageUrisToUpload = newImageList.filter { it.isNotEmpty() }
        uploadImagesToCloudinary(imageUrisToUpload) { uploadedImageUrls ->
            val allImageUrls = oldImageList.filter { it.isNotEmpty() } + uploadedImageUrls

            val reviewData = hashMapOf(
                "review_date" to reviewDate,
                "booth_cnt" to boothCnt,
                "accs_cnt" to accsCnt,
                "accs_condi" to accsCondi,
                "retouching" to retouching,
                "review_text" to reviewText,
                "review_rating" to reviewRating,
                "booth_id" to boothId,
                "user_id" to userId,
                "user_name" to userName,
                "photo_urls" to allImageUrls // Cloudinary URL 포함
            )

            // Firestore에 저장
            db.collection("review").document(reviewId).set(reviewData)
                .addOnSuccessListener {
                    updateBoothReview()
                    showToast("리뷰가 성공적으로 업데이트되었습니다.")
                    startActivity(Intent(this, MyReviewActivity::class.java))
                }
                .addOnFailureListener { showToast("리뷰 업데이트에 실패했습니다.") }
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

    private fun checkAndRequestPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        return if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)
            false
        } else true
    }

    private fun openGallery() = imagePickerResult.launch(Intent(Intent.ACTION_PICK).apply { type = "image/*" })

    private fun showErrorAndFinish(message: String) {
        showToast(message)
        finish()
    }

    private fun showToast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun showCancelDialog() {
        AlertDialog.Builder(this)
            .setTitle("리뷰 수정 취소")
            .setMessage("정말로 취소하시겠습니까? 취소하시면 현재 작성한 내용은 반영되지 않습니다.")
            .setPositiveButton("네") { _, _ -> finish() }
            .setNegativeButton("아니오", null)
            .create()
            .show()
    }

    private fun preselect(textViews: List<TextView>, selectedIndex: Int) {
        textViews.forEachIndexed { index, textView ->
            textView.setBackgroundResource(if (index == selectedIndex - 1) R.drawable.bg_dark_pink else R.drawable.bg_light_pink)
            textView.setTextColor(resources.getColor(if (index == selectedIndex - 1) R.color.white else R.color.black, null))
        }
    }

    private fun addSelectionListener(textViews: List<TextView>, onSelect: (Int) -> Unit) {
        textViews.forEachIndexed { index, textView ->
            textView.setOnClickListener {
                textViews.forEach {
                    it.setBackgroundResource(R.drawable.bg_light_pink)
                    it.setTextColor(resources.getColor(R.color.black, null))
                }
                textView.setBackgroundResource(R.drawable.bg_dark_pink)
                textView.setTextColor(resources.getColor(R.color.white, null))
                onSelect(index + 1)
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
                        val newRating = reviewRating // 수정된 리뷰의 새로운 점수
                        val oldReviewCnt = updateBooth.review_cnt ?: 0

                        // 새로운 review_avg 계산 (기존 점수에서 이전 리뷰 점수를 빼고 새로운 점수로 바꿈)
                        updateBooth.review_avg = ((oldRating * oldReviewCnt - oldRating + newRating) / oldReviewCnt)

                        // review_cnt는 수정되지 않음
                        updateBooth.review_cnt = oldReviewCnt

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