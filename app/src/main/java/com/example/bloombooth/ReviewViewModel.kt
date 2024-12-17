package com.example.bloombooth

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ReviewViewModel : ViewModel() {
    val imageList = MutableLiveData<MutableList<String>>().apply { value = mutableListOf() }
    val reviewText = MutableLiveData<String>().apply { value = "" }

    fun addImage(imageUri: String) {
        val currentList = imageList.value ?: mutableListOf()
        currentList.add(imageUri)
        imageList.value = currentList
    }

    fun removeImage(position: Int) {
        val currentList = imageList.value ?: mutableListOf()
        currentList.removeAt(position)
        imageList.value = currentList
    }

    fun saveReviewText(text: String) {
        reviewText.value = text
    }
}
