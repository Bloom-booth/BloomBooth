package com.example.bloombooth

import java.text.SimpleDateFormat
import java.util.*

object DateFormatter {
    private val dateFormat: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    }

    fun format(date: Date): String {
        return dateFormat.get().format(date)
    }
}
