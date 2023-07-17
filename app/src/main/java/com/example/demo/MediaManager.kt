package com.example.demo

object MediaManager {

    val backgroundMedias = mutableListOf<Media>()
    val frontMedias = mutableListOf<Media>()

    fun setBackGroundMedias(list: List<Media>) {
        backgroundMedias.clear()
        backgroundMedias.addAll(list)
    }

    fun setFrontMedias(list: List<Media>) {
        frontMedias.clear()
        frontMedias.addAll(list)
    }
}