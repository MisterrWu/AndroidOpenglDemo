package com.example.demo

import android.content.Context
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class PerspectiveInvertShape constructor(context: Context, isOES: Boolean = true) : BasePerspectiveShape(context,isOES) {

    private lateinit var textureBuffer: FloatBuffer
    private val textureVertexData = floatArrayOf(
        0f, 0f,
        1f, 0f,
        0f, 1f,
        1f, 1f
    )

    override fun initialize() {
        super.initialize()
        textureBuffer = ByteBuffer.allocateDirect(textureVertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(textureVertexData).also {
                it.position(0)
            }
    }

    override fun drawFrame(
        textureId: Int,
        mode: Int,
        vertexMatrix: FloatArray,
        textureMatrix: FloatArray,
        vertexBuffer: FloatBuffer,
        textureVertexBuffer: FloatBuffer
    ) {
        super.drawFrame(
            textureId,
            mode,
            vertexMatrix,
            textureMatrix,
            vertexBuffer,
            textureVertexBuffer = textureBuffer
        )
    }
}