package com.example.demo

class SelectBackgroundFragment : BaseSelectVideoFragment() {

    override fun getTitle(): CharSequence {
        return "选择背景视频"
    }

    override fun clickRightMenu() {
        val selectMedias = getSelectedMedias()
        if(selectMedias.isEmpty()) {
            showToast("请选择背景视频")
        } else {
            MediaManager.setBackGroundMedias(selectMedias)
            activity?.supportFragmentManager?.also {
                it.beginTransaction()
                    .replace(R.id.ll_container, SelectVideoFragment())
                    .commitAllowingStateLoss()
            }
        }
    }
}