package com.example.bloombooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bloombooth.databinding.ActivityOneReviewBinding

class OneReviewActivity : AppCompatActivity() {

    private val binding: ActivityOneReviewBinding by lazy { ActivityOneReviewBinding.inflate(layoutInflater) }
    private lateinit var adapter: ImageAdapter

    private val reviewViewModel: ReviewViewModel by viewModels()

    private val imagePickerResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                if ((reviewViewModel.imageList.value?.size ?: 0) < 3) {
                    reviewViewModel.addImage(imageUri.toString())
                } else {
                    showToast("이미지는 최대 3개까지 업로드할 수 있습니다.")
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupRecyclerView()

        binding.imageUploadBtn.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.pink)
        binding.uploadReviewFirst.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.pink)
        binding.goNextFirst.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.pink)

        val reviewEditText: EditText = binding.reviewText
        reviewEditText.addTextChangedListener {
            reviewViewModel.saveReviewText(it.toString())
        }

        binding.reviewBackBtn.setOnClickListener {
            val intent = Intent(this, DetailActivity::class.java)
            startActivity(intent)
        }

        binding.imageUploadBtn.setOnClickListener {
            if (checkAndRequestPermission()) {
                openGallery()
            }
        }

        binding.goNextFirst.setOnClickListener {
            val intent = Intent(this, TwoReviewActivity::class.java)
            startActivity(intent)
        }

        reviewViewModel.imageList.observe(this, Observer {
            adapter.notifyDataSetChanged()
        })

        reviewViewModel.reviewText.observe(this, Observer { text ->
            reviewEditText.setText(text)
        })
    }

    private fun setupRecyclerView() {
        adapter = ImageAdapter(reviewViewModel.imageList.value ?: mutableListOf(), { position ->
            reviewViewModel.removeImage(position)
        }, { uriString ->
            showToast("이미지 클릭: $uriString")
        })

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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                showToast("갤러리 접근 권한이 거부되었습니다.")
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}
