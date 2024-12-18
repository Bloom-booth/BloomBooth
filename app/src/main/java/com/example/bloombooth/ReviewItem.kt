package com.example.bloombooth

import java.util.Date

data class ReviewItem(
    val review_date: String = "",
    val booth_cnt: Int = 0,
    val accs_condi: Int = 0,
    val accs_cnt: Int = 0,
    val retouching: Int = 0,
    val review_rating: Int = 0,
    val booth_id: String = "",
    val user_id: String = "",
    val review_text: String = "",
    val photo_urls: List<String> = emptyList()
)