package com.example.demo

import android.content.Intent

class SelectVideoFragment : BaseSelectVideoFragment() {

    private var selectPosition = -1
    private var selectMedia: Media? = null

    override fun getTitle(): CharSequence {
        return "选择视频"
    }

    override fun clickRightMenu() {
        val selectMedias = getSelectedMedias()
        if(selectMedias.isEmpty()) {
            showToast("请选择视频")
        } else {
            MediaManager.setFrontMedias(selectMedias)
            activity?.apply {
                startActivity(Intent(this, PlayerActivity::class.java))
            }
        }
    }

    override fun onItemSelected(item: Media, position: Int) {
        if(position != selectPosition) {
            selectMedia?.isSelected = false
            if(selectPosition >= 0) adapter?.notifyItemChanged(selectPosition)

            selectPosition = position
            selectMedia = item
        }
    }
}