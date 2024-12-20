// SearchActivity.kt

package com.example.bloombooth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.bloombooth.databinding.ActivitySearchBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null

    private var accsCnt = 0
    private var accsCondi = 0
    private var retouching = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // MainActivity로 이동하기
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 현재 위치
        currentLatitude = intent.getDoubleExtra("currentLatitude", 0.0)
        currentLongitude = intent.getDoubleExtra("currentLongitude", 0.0)

        // 부스 최소 개수 드롭다운
        val boothCntValues = (1..10).toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, boothCntValues)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.boothCnt.adapter = adapter

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

        // 검색하기 버튼
        binding.btnSearch.setOnClickListener {
            val boothName = binding.boothName.text.toString()
            val boothCnt = binding.boothCnt.selectedItem as? Int

            val intent = Intent(this, SearchResultActivity::class.java).apply {
                // 검색 조건
                putExtra("boothName", boothName)
                putExtra("boothCnt", boothCnt)
                putExtra("accsCnt", accsCnt)
                putExtra("accsCondi", accsCondi)
                putExtra("retouching", retouching)
                // 현재 위치
                putExtra("currentLatitude",currentLatitude)
                putExtra("currentLongitude", currentLongitude)
            }
            startActivity(intent)
        }
    }

    // TextView 그룹 클릭 로직 설정
    private fun selectedToInt(
        textViews: List<TextView>,
        onSelect: (Int) -> Unit
    ) {
        var lastSelectedIndex: Int? = null // 마지막으로 선택된 항목 저장

        textViews.forEachIndexed { index, textView ->
            textView.setOnClickListener {
                if (lastSelectedIndex == index) {
                    // 이미 선택된 것을 다시 클릭한 경우 선택 해제
                    textViews[index].setBackgroundResource(R.drawable.background_round) // 기본 배경
                    textViews[index].setTextColor(resources.getColor(R.color.black, null)) // 기본 텍스트 색
                    lastSelectedIndex = null // 선택 해제
                    onSelect(0) // 값 초기화
                } else {
                    // 다른 항목 선택
                    textViews.forEach {
                        it.setBackgroundResource(R.drawable.background_round) // 기본 배경
                        it.setTextColor(resources.getColor(R.color.black, null)) // 기본 텍스트 색
                    }
                    textView.setBackgroundResource(R.drawable.dark_background_round) // 선택된 배경
                    textView.setTextColor(resources.getColor(R.color.white, null)) // 선택된 텍스트 색
                    lastSelectedIndex = index // 현재 선택 항목 업데이트
                    onSelect(index + 1) // 선택된 값 전달
                }
            }
        }
    }
}