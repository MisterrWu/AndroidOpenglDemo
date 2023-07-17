package com.example.demo

import android.content.Context

class CurvedShape constructor(context: Context, isOES: Boolean = false) : DefaultShape(context,isOES) {

    override var fragmentResId:Int = R.raw.curved_texture_shader
    override var vertexResId: Int = R.raw.curved_vertex_shader
}