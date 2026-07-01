package com.example.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val dateAdded: Long, // seconds
    val size: Long, // bytes
    val duration: Long? = null, // milliseconds for videos
    val width: Int = 0,
    val height: Int = 0,
    val mimeType: String = "image/jpeg",
    val location: String? = null,
    val isVideo: Boolean = false,
    val isFavorite: Boolean = false
) {
    // Computed property for duration string (e.g., "0:12")
    val durationString: String?
        get() {
            if (duration == null) return null
            val seconds = (duration / 1000) % 60
            val minutes = (duration / (1000 * 60)) % 60
            val hours = (duration / (1000 * 60 * 60)) % 24
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }

    // Friendly date grouping key (e.g., "Today", "Yesterday", "June 25, 2026")
    val friendlyDate: String
        get() {
            val millis = dateAdded * 1000L
            val now = System.currentTimeMillis()
            val diff = now - millis
            val oneDay = 24 * 60 * 60 * 1000L

            val calendar = java.util.Calendar.getInstance().apply { timeInMillis = millis }
            val nowCalendar = java.util.Calendar.getInstance().apply { timeInMillis = now }

            return when {
                diff in 0 until oneDay && calendar.get(java.util.Calendar.DAY_OF_YEAR) == nowCalendar.get(java.util.Calendar.DAY_OF_YEAR) -> "Today"
                diff in oneDay until (2 * oneDay) -> "Yesterday"
                else -> {
                    val sdf = java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(millis))
                }
            }
        }
}
