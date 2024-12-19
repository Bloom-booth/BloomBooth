package com.example.bloombooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.example.bloombooth.auth.FirebaseAuthManager
import com.example.bloombooth.databinding.ActivityReviewEditBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID

class ReviewEditActivity : AppCompatActivity() {
    val binding: ActivityReviewEditBinding by lazy { ActivityReviewEditBinding.inflate(layoutInflater) }
    private lateinit var numbersList: List<Int>
    val db = Firebase.firestore
    private var imageList = mutableListOf<String>()
    private var originImageUrls = mutableListOf<String>()
    var deleteImageUrls = mutableListOf<String>()
    private lateinit var adapter: ReviewEditImageAdapter
    private val newImages = mutableListOf<String>()


    private val imagePickerResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                if (newImages.size < 3) {
                    newImages.clear()
                    newImages.add(imageUri.toString())
                    adapter.notifyDataSetChanged()
                } else {
                    showToast("이미지는 최대 3개까지 업로드할 수 있습니다.")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        setupRecyclerView()
        defaultUISetting()
        boothCntSetting()
        addClickListeners()
        setupRadioButtons()

        val reviewText = intent.getStringExtra("review_text")
        val reviewRating = intent.getIntExtra("review_rating", 0)
        val boothId = intent.getStringExtra("booth_id")
        val boothCnt = intent.getIntExtra("booth_cnt", 0)
        val accsCnt = intent.getIntExtra("accs_cnt", 0)
        originImageUrls = intent.getStringArrayListExtra("photo_urls") ?: arrayListOf<String>()
        val accsCondi = intent.getIntExtra("accs_condi", 0)
        val retouching = intent.getIntExtra("retouching", 0)

        imageList  = originImageUrls
        boothId?.let {
            fetchBoothName(it)
        }

        val reviewTextView: TextView = binding.reviewText
        reviewText?.let {
            reviewTextView.text = it
        }

        fetchBoothCountText(boothCnt)

        val accsCntRadioGroup = binding.accsCntRadioGroup
        when (accsCnt) {
            1 -> accsCntRadioGroup.check(R.id.accsCnt1)
            2 -> accsCntRadioGroup.check(R.id.accsCnt2)
            3 -> accsCntRadioGroup.check(R.id.accsCnt3)
            4 -> accsCntRadioGroup.check(R.id.accsCnt4)
            else -> accsCntRadioGroup.clearCheck()
        }

        val accsCondiRadioGroup = binding.accsCondiRadioGroup
        when (accsCondi) {
            1 -> accsCondiRadioGroup.check(R.id.accsCondi1)
            2 -> accsCondiRadioGroup.check(R.id.accsCondi2)
            3 -> accsCondiRadioGroup.check(R.id.accsCondi3)
            4 -> accsCondiRadioGroup.check(R.id.accsCondi4)
            else -> accsCondiRadioGroup.clearCheck()
        }

        val retouchRadioGroup = binding.retouchRadioGroup
        when (retouching) {
            1 -> retouchRadioGroup.check(R.id.retouch1)
            2 -> retouchRadioGroup.check(R.id.retouch2)
            3 -> retouchRadioGroup.check(R.id.retouch3)
            4 -> retouchRadioGroup.check(R.id.retouch4)
            else -> retouchRadioGroup.clearCheck()
        }

        val reviewRatingBar = binding.ratingBar
        reviewRatingBar.rating = reviewRating.toFloat()
    }

    private fun fetchBoothCountText(boothCnt: Int) {
        if (boothCnt in 1..10) {
            binding.toggleButton.text = boothCnt.toString()
        }
    }

    private fun fetchBoothName(boothId: String) {
        val boothRef = db.collection("booth").document(boothId)
        boothRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val boothName = document.getString("booth_name")
                    boothName?.let {
                        binding.boothName.text = it
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ReviewActivity", "Error getting document: ", exception)
            }
    }

    private fun boothCntSetting() {
        numbersList = (1..10).toList()
    }

    private fun addClickListeners() {
        binding.reviewEditBackBtn.setOnClickListener {
            val dialog = AlertDialog.Builder(this)
                .setTitle("리뷰 수정 취소")
                .setMessage("정말로 취소하시겠습니까? 취소하시면 현재 작성한 내용은 반영되지 않습니다.")
                .setPositiveButton("네") { _, _ ->
                    finish()
                }
                .setNegativeButton("아니오", null)
                .create()
            dialog.show()
        }

        binding.imageUploadBtn.setOnClickListener {
            if (checkAndRequestPermission()) {
                openGallery()
            }
        }

        binding.uploadReviewBtn.setOnClickListener {
            if (validateRating()) {
                saveReview()
            } else {
                showToast("평점을 입력해 주세요.")
            }
        }

        binding.toggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showNumberDialog()
            } else {
                binding.numbersRecyclerView.visibility = View.GONE
            }
        }
    }

    private fun saveReview() {
        val userId = FirebaseAuthManager.auth.currentUser?.uid ?: return userNullAction()
        val boothId = intent.getStringExtra("booth_id") ?: return boothNullAction()
        val reviewDate = DateFormatter.format(Date())
        val reviewText = binding.reviewText.text.toString()
        val rating = binding.ratingBar.rating.toInt()

        val selectedBoothCount = if (binding.toggleButton.text.toString() == "선택") 0 else binding.toggleButton.text.toString().toIntOrNull() ?: 0
        val selectedAccsCnt = getSelectedRadioButtonId(binding.accsCntRadioGroup)
        val selectedAccsCondi = getSelectedRadioButtonId(binding.accsCondiRadioGroup)
        val selectedRetouch = getSelectedRadioButtonId(binding.retouchRadioGroup)

        val userRef = db.collection("user").document(userId)
        userRef.get().addOnSuccessListener { documentSnapshot ->
            val userName = documentSnapshot.getString("nickname") ?: "알수없음"
            val currentImageUrls = intent.getStringArrayListExtra("photo_urls") ?: arrayListOf<String>()
            val updatedImageUrls = currentImageUrls.toMutableList().apply {
                removeAll(deleteImageUrls)
            }
            updateReviewImages()
            saveReviewData(updatedImageUrls, userName, reviewText, rating, selectedBoothCount, selectedAccsCnt, selectedAccsCondi, selectedRetouch, boothId, reviewDate)

            if (newImages.isNotEmpty()) {
                uploadNewImages(updatedImageUrls, userName, reviewText, rating, selectedBoothCount, selectedAccsCnt, selectedAccsCondi, selectedRetouch, boothId, reviewDate)
            } else {
                saveReviewData(updatedImageUrls, userName, reviewText, rating, selectedBoothCount, selectedAccsCnt, selectedAccsCondi, selectedRetouch, boothId, reviewDate)
            }

        }.addOnFailureListener { e ->
            showToast("사용자 정보를 불러오는 데 실패했습니다.")
            Log.e("YEONJAE", "사용자 정보 불러오기 실패: ${e.localizedMessage}")
        }
    }

    private fun updateReviewImages() {
        val boothRef = db.collection("review").document(intent.getStringExtra("review_id")!!)

        boothRef.update("photo_urls", FieldValue.arrayRemove(*deleteImageUrls.toTypedArray()))
            .addOnSuccessListener {
                Log.d("YEONJAE", "이미지 URL 삭제 완료 및 업데이트: 삭제된 이미지 URL 리스트 - ${deleteImageUrls.joinToString(", ")}")
            }
            .addOnFailureListener { e ->
                Log.e("YEONJAE", "이미지 URL 삭제 실패 및 업데이트 오류: ${e.localizedMessage}")
            }
    }

    private fun uploadNewImages(updatedImageUrls: MutableList<String>, userName: String, reviewText: String, rating: Int, selectedBoothCount: Int, selectedAccsCnt: Int, selectedAccsCondi: Int, selectedRetouch: Int, boothId: String?, reviewDate: String) {
        val cloudinary = Cloudinary(
            ObjectUtils.asMap(
                "cloud_name", getString(R.string.cloud_name),
                "api_key", getString(R.string.image_api_key),
                "api_secret", getString(R.string.image_api_secret)
            )
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uploadedUrls = newImages.map { imageUri ->
                    val uri = Uri.parse(imageUri)
                    val inputStream = contentResolver.openInputStream(uri)
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

                updatedImageUrls.addAll(uploadedUrls)
                withContext(Dispatchers.Main) {
                    saveReviewData(updatedImageUrls, userName, reviewText, rating, selectedBoothCount, selectedAccsCnt, selectedAccsCondi, selectedRetouch, boothId, reviewDate)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("이미지 업로드에 실패했습니다. 오류: ${e.message}")
                    Log.e("YEONJAE", "이미지 업로드 실패: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun saveReviewData(updatedImageUrls: List<String>, userName: String, reviewText: String, rating: Int, selectedBoothCount: Int, selectedAccsCnt: Int, selectedAccsCondi: Int, selectedRetouch: Int, boothId: String?, reviewDate: String) {
        val reviewData = hashMapOf(
            "review_date" to reviewDate,
            "booth_cnt" to selectedBoothCount,
            "accs_condi" to selectedAccsCondi,
            "accs_cnt" to selectedAccsCnt,
            "retouching" to selectedRetouch,
            "review_text" to reviewText,
            "review_rating" to rating,
            "booth_id" to boothId,
            "user_id" to FirebaseAuthManager.auth.currentUser?.uid,
            "user_name" to userName,
            "photo_urls" to updatedImageUrls
        )

        val reviewId = intent.getStringExtra("review_id")
        if (reviewId != null) {
            db.collection("review").document(reviewId)
                .set(reviewData)
                .addOnSuccessListener {
                    showToast("리뷰가 성공적으로 업데이트되었습니다.")
                    finish()
                }
                .addOnFailureListener { e ->
                    showToast("리뷰 업데이트에 실패했습니다. 다시 시도해주세요.")
                    Log.e("YEONJAE", "리뷰 업데이트 실패: ${e.localizedMessage}")
                }
        } else {
            showToast("존재하지 않는 리뷰입니다! 다시 시도해주세요.")
        }
    }

    private fun validateRating(): Boolean {
        val rating = binding.ratingBar.rating
        return rating > 0
    }

    private fun boothNullAction() {
        showToast("해당 부스가 존재하지 않습니다.")
        startActivity(Intent(this, SplashActivity::class.java))
        finish()
    }

    private fun userNullAction() {
        showToast("로그인이 필요합니다.")
        startActivity(Intent(this, SplashActivity::class.java))
        finish()
    }

    private fun getSelectedRadioButtonId(radioGroup: RadioGroup): Int {
        val selectedRadioButtonId = radioGroup.checkedRadioButtonId
        val selectedRadioButton = findViewById<RadioButton>(selectedRadioButtonId)
        val selectedText = selectedRadioButton?.text.toString()

        return when (selectedText) {
            "없음" -> 1
            "적음" -> 2
            "나쁨" -> 2
            "보통" -> 3
            "좋음" -> 4
            "많음" -> 4
            else -> 0
        }
    }

    private fun showNumberDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("부스 개수 선택")
            .setItems(numbersList.map { it.toString() }.toTypedArray()) { _, which ->
                val selectedNumber = numbersList[which]
                binding.toggleButton.text = selectedNumber.toString()
            }
            .setNegativeButton("취소", null)
            .create()

        dialog.show()
    }

    private fun defaultUISetting() {
        binding.imageUploadBtn.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.pink)
        binding.uploadReviewBtn.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.pink)
    }

    private fun setupRecyclerView() {
        val imageList = ArrayList<String>(intent.getStringArrayListExtra("photo_urls") ?: arrayListOf())

        originImageUrls = imageList

        adapter = ReviewEditImageAdapter(
            images = originImageUrls,
            deletedImages = deleteImageUrls
        )
        binding.imageThumbnailList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.imageThumbnailList.adapter = adapter
    }

    private fun checkAndRequestPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    PERMISSION_REQUEST_CODE
                )
                return false
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
                return false
            }
        }
        return true
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        imagePickerResult.launch(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setupRadioButtons() {
        val accsCntRadioGroup = binding.accsCntRadioGroup
        val accsCondiRadioGroup = binding.accsCondiRadioGroup
        val retouchRadioGroup = binding.retouchRadioGroup

        accsCntRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            changeRadioButtonColor(group, checkedId)
        }
        accsCondiRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            changeRadioButtonColor(group, checkedId)
        }
        retouchRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            changeRadioButtonColor(group, checkedId)
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun changeRadioButtonColor(group: RadioGroup, checkedId: Int) {
        val selectedRadioButton = group.findViewById<RadioButton>(checkedId)

        selectedRadioButton.setBackgroundResource(R.drawable.main_button)
        for (i in 0 until group.childCount) {
            val button = group.getChildAt(i) as RadioButton
            if (button != selectedRadioButton) {
                button.setBackgroundResource(R.drawable.edit_text)
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}
