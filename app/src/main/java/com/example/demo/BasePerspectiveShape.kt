package com.example.demo

import android.content.Context
import android.opengl.GLES30
import android.opengl.Matrix.*


abstract class BasePerspectiveShape constructor(context: Context, isOES: Boolean = true) : DefaultShape(context,isOES){

    override var vertexResId: Int = R.raw.perspective_vertex_shader
    private var uPMatrixHandle = 0

    private val projectionMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    override fun initialize() {
        super.initialize()
        uPMatrixHandle = GLES30.glGetUniformLocation(program, "uProjectionMatrix")
        setIdentityM(modelMatrix, 0)
    }

    // 设置透视投影矩阵
    fun setPerspectiveM(width: Int, height: Int, a:Float) {
        val aspect = width.toFloat() / height.toFloat()

        perspectiveM(projectionMatrix, 0, 45f, aspect, 1f, 10f)
        translateM(modelMatrix, 0, 0f, 0f, -2.5f)
        rotateM(modelMatrix, 0, a, 0f, 1f, 0f)

        val temp = FloatArray(16)
        multiplyMM(temp, 0, projectionMatrix, 0, modelMatrix, 0)
        System.arraycopy(temp, 0, projectionMatrix, 0, temp.size)
    }

    override fun setupMatrix() {
        GLES30.glUniformMatrix4fv(uPMatrixHandle, 1, false, projectionMatrix, 0)
    }
}