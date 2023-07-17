package com.example.demo

import android.content.Context

class PerspectiveShape constructor(context: Context, isOES: Boolean = true) : BasePerspectiveShape(context,isOES) {

    override var fragmentResId: Int = R.raw.perspective_texture_shader
}