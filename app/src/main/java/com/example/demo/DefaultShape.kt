package com.example.demo

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.util.Log
import java.nio.FloatBuffer

open class DefaultShape constructor(context: Context,private val isOES: Boolean = true) : BaseShape(context){
    
    protected var program = 0
    private var positionHandle = 0
    private var uMatrixHandle = 0
    private var uSTMatrixHandle = 0
    private var texCoordsHandle = 0
    private var textureHandle = 0

    protected open var vertexResId: Int = R.raw.default_vertex_2d_shader
    protected open var fragmentResId: Int = R.raw.default_texture_shader

    override fun initialize() {
        program = GlUtil.createProgram(context, vertexResId, fragmentResId, isOES)
        positionHandle = GLES30.glGetAttribLocation(program, "aPosition")
        uMatrixHandle = GLES30.glGetUniformLocation(program, "uMatrix")
        uSTMatrixHandle = GLES30.glGetUniformLocation(program, "uSTMatrix")
        textureHandle = GLES30.glGetUniformLocation(program, "texture_0")
        texCoordsHandle = GLES30.glGetAttribLocation(program, "aTexCoord")
        Log.d("wuhan", "initialize: $textureHandle")
        GlUtil.checkGlError(" createProgram")
    }

    override fun drawFrame(
        textureId: Int,
        mode: Int,
        vertexMatrix: FloatArray,
        textureMatrix: FloatArray,
        vertexBuffer: FloatBuffer,
        textureVertexBuffer: FloatBuffer
    ) {
        GLES30.glUseProgram(program)

        setupMatrix()
        GLES30.glUniformMatrix4fv(uMatrixHandle, 1, false, vertexMatrix, 0)
        GLES30.glUniformMatrix4fv(uSTMatrixHandle, 1, false, textureMatrix, 0)

        vertexBuffer.position(0)
        GLES30.glEnableVertexAttribArray(positionHandle)
        GLES30.glVertexAttribPointer(positionHandle, getVertexSize(), GLES30.GL_FLOAT, false, 0, vertexBuffer)

        textureVertexBuffer.position(0)
        GLES30.glEnableVertexAttribArray(texCoordsHandle)
        GLES30.glVertexAttribPointer(texCoordsHandle, 2, GLES30.GL_FLOAT, false, 0, textureVertexBuffer)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        if (isOES) {
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        } else {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        }

        GLES30.glUniform1i(textureHandle, 0)
        GLES30.glDrawArrays(mode, 0, vertexBuffer.capacity() / getVertexSize())

        GLES30.glDisableVertexAttribArray(positionHandle)
        GLES30.glDisableVertexAttribArray(texCoordsHandle)
    }

    protected open fun getVertexSize(): Int {
        return 3
    }

    protected open fun setupMatrix() {}

    override fun release() {
        if (program >= 0) {
            try {
                GLES30.glDeleteProgram(program)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            program = -1
        }
    }
}