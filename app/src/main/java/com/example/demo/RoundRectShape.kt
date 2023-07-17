package com.example.demo

import android.content.Context

class RoundRectShape constructor(context: Context, private val isOES: Boolean = true) : DefaultShape(context) {

    override var fragmentResId: Int = R.raw.round_rect_texture_shader
}