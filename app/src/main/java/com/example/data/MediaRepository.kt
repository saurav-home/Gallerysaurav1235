package com.example.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaRepository(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver

    suspend fun fetchLocalMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()

        // 1. Fetch Images
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE
        )

        val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            contentResolver.query(
                imageUri,
                imageProjection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Image_$id"
                    val dateAdded = cursor.getLong(dateColumn)
                    val size = cursor.getLong(sizeColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val mimeType = cursor.getString(mimeColumn) ?: "image/jpeg"
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    mediaList.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            name = name,
                            dateAdded = dateAdded,
                            size = size,
                            width = width,
                            height = height,
                            mimeType = mimeType,
                            location = "Local Device",
                            isVideo = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MediaRepository", "Error querying images from MediaStore", e)
        }

        // 2. Fetch Videos
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION
        )

        val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        try {
            contentResolver.query(
                videoUri,
                videoProjection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Video_$id"
                    val dateAdded = cursor.getLong(dateColumn)
                    val size = cursor.getLong(sizeColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val mimeType = cursor.getString(mimeColumn) ?: "video/mp4"
                    val duration = cursor.getLong(durationColumn)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                    mediaList.add(
                        MediaItem(
                            id = id + 10000000L, // distinct namespace for ids
                            uri = contentUri,
                            name = name,
                            dateAdded = dateAdded,
                            size = size,
                            duration = duration,
                            width = width,
                            height = height,
                            mimeType = mimeType,
                            location = "Local Device",
                            isVideo = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MediaRepository", "Error querying videos from MediaStore", e)
        }

        // Sort combined list descending by date added
        mediaList.sortByDescending { it.dateAdded }

        // If list is empty (e.g. running on an empty dev emulator), return gorgeous mock items for design fidelity
        if (mediaList.isEmpty()) {
            return@withContext getMockMedia()
        }

        mediaList
    }

    // Helper to generate a complete catalog of beautiful mock photos and videos
    private fun getMockMedia(): List<MediaItem> {
        val nowSeconds = System.currentTimeMillis() / 1000L
        val oneDaySeconds = 24 * 60 * 60L

        val mockPhotos = listOf(
            // Yosemite Mountain - Today
            MockData(
                id = 1L,
                url = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1200",
                name = "Yosemite Valley.jpg",
                dateOffset = 0, // Today
                size = 3546782,
                width = 4032,
                height = 3024,
                location = "Yosemite National Park, CA"
            ),
            // Big Buck Bunny - Video Today
            MockData(
                id = 2L,
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                name = "Big Buck Bunny Short.mp4",
                dateOffset = 0, // Today
                size = 15843212,
                width = 1920,
                height = 1080,
                duration = 596000L,
                mimeType = "video/mp4",
                isVideo = true,
                location = "Peach Open Movie Studio"
            ),
            // Beach Sunset - Today
            MockData(
                id = 3L,
                url = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200",
                name = "Sunset Beach.jpg",
                dateOffset = 0, // Today
                size = 2145601,
                width = 4000,
                height = 3000,
                location = "Maui, Hawaii"
            ),
            // Cute Puppy - Yesterday
            MockData(
                id = 4L,
                url = "https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=1200",
                name = "Golden Retriever Puppy.jpg",
                dateOffset = 1, // Yesterday
                size = 1894562,
                width = 3024,
                height = 4032,
                location = "Golden Gate Park, SF"
            ),
            // Sintel - Video Yesterday
            MockData(
                id = 5L,
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                name = "Sintel Cinematic Project.mp4",
                dateOffset = 1, // Yesterday
                size = 42531647,
                width = 1280,
                height = 544,
                duration = 52000L,
                mimeType = "video/mp4",
                isVideo = true,
                location = "Durian Open Movie Project"
            ),
            // Tokyo Neon - Yesterday
            MockData(
                id = 6L,
                url = "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=1200",
                name = "Shibuya Neon Crossing.jpg",
                dateOffset = 1, // Yesterday
                size = 4851234,
                width = 6000,
                height = 4000,
                location = "Shibuya, Tokyo, Japan"
            ),
            // Cozy Cabin - 4 days ago
            MockData(
                id = 7L,
                url = "https://images.unsplash.com/photo-1510798831971-661eb04b3739?w=1200",
                name = "Winter Cabin Retreat.jpg",
                dateOffset = 4,
                size = 2841203,
                width = 3840,
                height = 2160,
                location = "Zermatt, Switzerland"
            ),
            // New York Skyline - 4 days ago
            MockData(
                id = 8L,
                url = "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=1200",
                name = "Manhattan Golden Hour.jpg",
                dateOffset = 4,
                size = 3125468,
                width = 4000,
                height = 2667,
                location = "Brooklyn Bridge, NY"
            ),
            // Espresso Art - 7 days ago
            MockData(
                id = 9L,
                url = "https://images.unsplash.com/photo-1497034825429-c343d7c6a68f?w=1200",
                name = "Flat White Pour.jpg",
                dateOffset = 7,
                size = 1452680,
                width = 3264,
                height = 2448,
                location = "Verve Coffee Roasters"
            ),
            // Forest - 10 days ago
            MockData(
                id = 10L,
                url = "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=1200",
                name = "Redwood Sanctuary.jpg",
                dateOffset = 10,
                size = 5612348,
                width = 5472,
                height = 3648,
                location = "Muir Woods, CA"
            ),
            // Desert Dunes - 10 days ago
            MockData(
                id = 11L,
                url = "https://images.unsplash.com/photo-1509316975850-ff9c5edd0cd9?w=1200",
                name = "Sahara Golden Dunes.jpg",
                dateOffset = 10,
                size = 2364125,
                width = 4000,
                height = 2667,
                location = "Merzouga, Morocco"
            ),
            // Retro Car - 15 days ago
            MockData(
                id = 12L,
                url = "https://images.unsplash.com/photo-1525609004556-c46c7d6cf0a3?w=1200",
                name = "Classic Cabriolet.jpg",
                dateOffset = 15,
                size = 3894215,
                width = 4500,
                height = 3000,
                location = "Havana, Cuba"
            ),
            // Lavender Fields - 20 days ago
            MockData(
                id = 13L,
                url = "https://images.unsplash.com/photo-1528183429752-a97d0bf99b5a?w=1200",
                name = "Provence Fields.jpg",
                dateOffset = 20,
                size = 4120394,
                width = 5184,
                height = 3456,
                location = "Valensole, France"
            ),
            // Snowy Peak - 25 days ago
            MockData(
                id = 14L,
                url = "https://images.unsplash.com/photo-1486915309851-b0cc1f8a0084?w=1200",
                name = "Summit View.jpg",
                dateOffset = 25,
                size = 2951047,
                width = 4000,
                height = 3000,
                location = "Mount Rainier, WA"
            ),
            // Venice Canal - 30 days ago
            MockData(
                id = 15L,
                url = "https://images.unsplash.com/photo-1519112232436-9923c6192a53?w=1200",
                name = "Venetian Waterway.jpg",
                dateOffset = 30,
                size = 3421508,
                width = 3840,
                height = 2560,
                location = "Venice, Italy"
            )
        )

        return mockPhotos.map { mock ->
            MediaItem(
                id = mock.id,
                uri = Uri.parse(mock.url),
                name = mock.name,
                dateAdded = nowSeconds - (mock.dateOffset * oneDaySeconds),
                size = mock.size,
                duration = mock.duration,
                width = mock.width,
                height = mock.height,
                mimeType = mock.mimeType,
                location = mock.location,
                isVideo = mock.isVideo
            )
        }
    }

    private data class MockData(
        val id: Long,
        val url: String,
        val name: String,
        val dateOffset: Int,
        val size: Long,
        val width: Int,
        val height: Int,
        val duration: Long? = null,
        val mimeType: String = "image/jpeg",
        val isVideo: Boolean = false,
        val location: String? = null
    )
}
