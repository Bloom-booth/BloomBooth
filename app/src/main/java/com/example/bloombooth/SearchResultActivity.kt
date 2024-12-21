package com.example.bloombooth

import Booth
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bloombooth.databinding.ActivitySearchResultBinding
import com.google.firebase.firestore.FirebaseFirestore

class SearchResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchResultBinding
    private lateinit var adapter: SearchResultAdapter

    private val boothList1 = mutableListOf<Booth>()
    private val boothList2 = mutableListOf<Booth>()

    private val currentLatitude: Double = intent?.getDoubleExtra("currentLatitude", 0.0) ?: 0.0
    private val currentLongitude: Double = intent?.getDoubleExtra("currentLongitude", 0.0) ?: 0.0

    private val currentLocation = Location("").apply {
        latitude = currentLatitude
        longitude = currentLongitude
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = SearchResultAdapter { booth ->
            openDetailActivity(booth)
        }

        binding.searchResult.layoutManager = LinearLayoutManager(this)
        binding.searchResult.adapter = adapter

        binding.icBack.setOnClickListener { finish() }
        binding.btnBack.setOnClickListener { finish() }

        searchBooth()
    }

    private fun searchBooth() {
        val boothName = intent.getStringExtra("boothName")
        val boothCnt = intent.getIntExtra("boothCnt", 0)
        val accsCnt = intent.getIntExtra("accsCnt", 0)
        val accsCondi = intent.getIntExtra("accsCondi", 0)
        val retouching = intent.getIntExtra("retouching", 0)

        val db = FirebaseFirestore.getInstance()
        db.collection("booth")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("SearchResultTest", "No documents found")
                } else {
                    Log.d("SearchResultTest", "Found ${documents.size()} matching documents")
                    for (document in documents) {
                        // Booth 객체로 변환
                        val booth = document.toObject(Booth::class.java)
                        booth.booth_id = document.id

                        if (boothName != null) {
                            if (
                                booth.booth_name.contains(boothName, ignoreCase = true) &&
                                booth.booth_cnt >= boothCnt &&
                                booth.accs_cnt >= accsCnt &&
                                booth.accs_condi >= accsCondi &&
                                booth.retouching >= retouching
                            ) boothList1.add(booth)
                        } else {
                            if (
                                booth.booth_cnt >= boothCnt &&
                                booth.accs_cnt >= accsCnt &&
                                booth.accs_condi >= accsCondi &&
                                booth.retouching >= retouching
                            ) boothList1.add(booth)
                        }
                    }
                    sortDistance()
                    sortReviewAvg()
                    updateUI(boothList1)
                    toggleGroup()
                }
            }
            .addOnFailureListener { e ->
                Log.e("SearchResultTest", "Failed to fetch data", e)
            }
    }

    private fun sortDistance() {
        boothList1.sortWith(Comparator { booth1, booth2 ->
            val distance1 = calculateDistance(booth1) ?: Float.MAX_VALUE
            val distance2 = calculateDistance(booth2) ?: Float.MAX_VALUE
            distance2.compareTo(distance1)
        })
    }

    private fun sortReviewAvg() {
        boothList2.clear()
        boothList2.addAll(boothList1)
        boothList2.sortWith(Comparator { booth1, booth2 ->
            val reviewAvgComparison = booth2.review_avg.compareTo(booth1.review_avg)
            if (reviewAvgComparison != 0) {
                reviewAvgComparison
            } else {
                val distance1 = calculateDistance(booth1) ?: Float.MAX_VALUE
                val distance2 = calculateDistance(booth2) ?: Float.MAX_VALUE
                distance1.compareTo(distance2)
            }
        })
    }

    private fun calculateDistance(booth: Booth): Float? {
        val boothLocation = booth.getLocation() ?: return null
        return currentLocation.distanceTo(boothLocation)
    }

    private fun updateUI(boothList: List<Booth>) {
        adapter.submitList(boothList)
    }

    private fun toggleGroup() {
        binding.toggleSort.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.button_distance -> updateUI(boothList1)
                    R.id.button_review -> updateUI(boothList2)
                }
            }
        }
    }

    private fun openDetailActivity(booth: Booth) {
        val intent = Intent(this, DetailActivity::class.java)
        intent.putExtra("boothNumber", booth.booth_number)
        intent.putExtra("boothName", booth.booth_name)
        intent.putExtra("boothId", booth.booth_id)
        startActivity(intent)
    }
}