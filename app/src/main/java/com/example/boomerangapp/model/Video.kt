package com.example.boomerangapp.model

import com.google.firebase.Timestamp

data class Video(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val videoUrl: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val likes: Int = 0,
    val comments: List<Comment> = emptyList(),
    val caption: String = ""
)

data class Comment(
    val userId: String = "",
    val username: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now()
)
