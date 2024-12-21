package com.example.bloombooth

data class Review(
    val accs_cnt: Int = 0,
    val accs_condi: Int = 0,
    val booth_cnt: Int = 0,
    val booth_id: String = "",
    val photo_urls: List<String> = emptyList(),
    val retouching: Int = 0,
    val review_date: String = "",
    val review_rating: Int = 0,
    val review_text: String = "",
    val user_id: String = "",
    val user_name: String = "",
) {
    var booth_name: String = ""
    var review_id: String = ""
}