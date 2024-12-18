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
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bloombooth.auth.FirebaseAuthManager
import com.example.bloombooth.databinding.ActivityReviewBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.util.Date
import com.cloudinary.Cloudinary
import kotlinx.coroutines.*
import com.cloudinary.utils.ObjectUtils
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class ReviewActivity : AppCompatActivity() {
    private val binding: ActivityReviewBinding by lazy { ActivityReviewBinding.inflate(layoutInflater) }
    private val imageList = mutableListOf<String>()
    private lateinit var adapter: ImageAdapter
    private lateinit var numbersList: List<Int>

    private val imagePickerResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                if (imageList.size < 3) {
                    imageList.add(imageUri.toString())
                    adapter.notifyItemInserted(imageList.size - 1)
                } else {
                    showToast("이미지는 최대 3개까지 업로드할 수 있습니다.")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val boothName = intent.getStringExtra("boothName") ?: "업체명"
        binding.boothName.text = boothName

        setupRecyclerView()
        defaultUISetting()
        boothCntSetting()
        addClickListeners()
        setupRadioButtons()
    }

    private fun boothCntSetting() {
        numbersList = (1..10).toList()
    }

    private fun addClickListeners() {
        binding.reviewBackBtn.setOnClickListener {
            val intent = Intent(this, DetailActivity::class.java)
            startActivity(intent)
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
        val db = Firebase.firestore
        val selectedBoothCount = if (binding.toggleButton.text.toString() == "선택") {
            0
        } else {
            binding.toggleButton.text.toString().toIntOrNull() ?: 0
        }

        val selectedAccsCnt = getSelectedRadioButtonId(binding.accsCntRadioGroup)
        val selectedAccsCondi = getSelectedRadioButtonId(binding.accsCondiRadioGroup)
        val selectedRetouch = getSelectedRadioButtonId(binding.retouchRadioGroup)
        val reviewText = binding.reviewText.text.toString()
        val rating = binding.ratingBar.rating.toInt()

        val userId = FirebaseAuthManager.auth.currentUser?.uid
        if (userId == null) userNullAction()

        val boothId = intent.getStringExtra("boothId")
        if (boothId == null) boothNullAction()

        val reviewDate = DateFormatter.format(Date())

        val userRef = db.collection("user").document(userId!!)
        userRef.get().addOnSuccessListener { documentSnapshot ->
            val userName = documentSnapshot.getString("nickname") ?: "알수없음"

            uploadImagesToCloudinary(imageList) { urls ->
                val reviewData = hashMapOf(
                    "review_date" to reviewDate,
                    "booth_cnt" to selectedBoothCount,
                    "accs_condi" to selectedAccsCondi,
                    "accs_cnt" to selectedAccsCnt,
                    "retouching" to selectedRetouch,
                    "review_text" to reviewText,
                    "review_rating" to rating,
                    "booth_id" to boothId,
                    "user_id" to userId,
                    "user_name" to userName,
                    "photo_urls" to urls
                )

                db.collection("review")
                    .add(reviewData)
                    .addOnSuccessListener { documentReference ->
                        val reviewId = documentReference.id

                        if (userId != null) {
                            db.collection("user").document(userId)
                                .update("review_ids", FieldValue.arrayUnion(reviewId))
                                .addOnSuccessListener {
                                    showToast("리뷰가 성공적으로 등록되었습니다.")
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    showToast("리뷰 등록에 실패했습니다. 다시 시도해주세요.")
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        showToast("리뷰 등록에 실패했습니다. 다시 시도해주세요.")
                    }
            }
        }.addOnFailureListener { e ->
            showToast("사용자 정보를 불러오는 데 실패했습니다.")
        }
    }


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
        adapter = ImageAdapter(imageList, { position ->
            imageList.removeAt(position)
            adapter.notifyItemRemoved(position)
        }, { _ -> })
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