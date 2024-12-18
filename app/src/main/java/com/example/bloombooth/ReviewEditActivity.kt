package com.example.bloombooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bloombooth.databinding.ActivityReviewEditBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class ReviewEditActivity : AppCompatActivity() {
    val binding: ActivityReviewEditBinding by lazy { ActivityReviewEditBinding.inflate(layoutInflater) }
    private lateinit var numbersList: List<Int>
    val db = Firebase.firestore
    private val imageList = mutableListOf<String>()
    private lateinit var adapter: ImageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        boothCntSetting()
        setupRecyclerView()
        defaultUISetting()
        boothCntSetting()
        addClickListeners()
        setupRadioButtons()

        val reviewId = intent.getStringExtra("review_id")
        val reviewText = intent.getStringExtra("review_text")
        val reviewRating = intent.getIntExtra("review_rating", 0)
        val reviewDate = intent.getStringExtra("review_date")
        val boothId = intent.getStringExtra("booth_id")
        val boothCnt = intent.getIntExtra("booth_cnt", 0)
        val accsCnt = intent.getIntExtra("accs_cnt", 0)
        val photoUrls = intent.getStringArrayListExtra("photo_urls") ?: arrayListOf<String>()
        val accsCondi = intent.getIntExtra("accs_condi", 0)
        val retouching = intent.getIntExtra("retouching", 0)

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
        // numbersList 초기화
        numbersList = (1..10).toList()
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

        binding.toggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showNumberDialog()
            } else {
                binding.numbersRecyclerView.visibility = View.GONE
            }
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
    }

    private fun openGallery() {
        TODO("Not yet implemented")
    }

    private fun validateRating(): Boolean {
        TODO("Not yet implemented")
    }

    private fun saveReview() {
        TODO("Not yet implemented")
    }

    private fun defaultUISetting() {
        binding.imageUploadBtn.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.pink)
        binding.uploadReviewBtn.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.pink)
    }

    private fun setupRecyclerView() {
        val imageList = ArrayList<String>(intent.getStringArrayListExtra("photo_urls") ?: arrayListOf())
        adapter = ImageAdapter(imageList, { position ->
            imageList.removeAt(position)
            adapter.notifyItemRemoved(position)
        }, { _ -> })

        // RecyclerView 설정
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
