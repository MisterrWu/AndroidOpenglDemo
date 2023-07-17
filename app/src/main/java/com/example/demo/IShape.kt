package com.example.demo

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

interface IShape {

    companion object{
        val defaultVertexMatrix = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )

        val defaultTextureMatrix = floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, -1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 1.0f
        )

        var defaultTextureVertexBuffer: FloatBuffer
        var defaultVertexBuffer: FloatBuffer

        private val defaultVertexData = floatArrayOf(
            1f, -1f, 0f,
            -1f, -1f, 0f,
            1f, 1f, 0f,
            -1f, 1f, 0f
        )

        private val defaultTextureVertexData = floatArrayOf(
            1f, 0f,
            0f, 0f,
            1f, 1f,
            0f, 1f
        )
        init {
            defaultVertexBuffer = ByteBuffer.allocateDirect(defaultVertexData.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(defaultVertexData).also {
                    it.position(0)
                }
            defaultTextureVertexBuffer = ByteBuffer.allocateDirect(defaultTextureVertexData.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(defaultTextureVertexData).also {
                    it.position(0)
                }
        }
    }

    fun init()
    fun isInitialized(): Boolean
    fun drawFrame(textureId: Int,
                  mode:Int = GLES30.GL_TRIANGLE_STRIP,
                  vertexMatrix: FloatArray = defaultVertexMatrix,
                  textureMatrix: FloatArray = defaultTextureMatrix,
                  vertexBuffer: FloatBuffer = defaultVertexBuffer,
                  textureVertexBuffer: FloatBuffer = defaultTextureVertexBuffer
    )
    fun release()
}