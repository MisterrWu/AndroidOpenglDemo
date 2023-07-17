package com.example.demo;

import android.opengl.Matrix;
import android.util.Log;

import java.util.Arrays;
import java.util.Stack;

public class MatrixTools {
    private static final String TAG = "MatrixTools";
    private float[] mMatrixProjection = new float[16];    //投影矩阵
    private float[] mMatrixTransform =     //原始矩阵
                   {1, 0, 0, 0,
                    0, 1, 0, 0,
                    0, 0, 1, 0,
                    0, 0, 0, 1};

    private Stack<float[]> mStack;      //变换矩阵堆栈

    public MatrixTools() {
        mStack = new Stack<>();
    }

    //保护现场
    public void pushMatrix() {
        mStack.push(Arrays.copyOf(mMatrixTransform, 16));
    }

    //恢复现场
    public void popMatrix() {
        mMatrixTransform = mStack.pop();
    }

    public void peekMatrix() {
        float[] values = mStack.peek();
        mMatrixTransform = Arrays.copyOf(values, values.length);
    }

    public void clearStack() {
        mStack.clear();
    }

    //平移变换
    public void translate(float x, float y, float z) {
        Matrix.translateM(mMatrixTransform, 0, x, y, z);
    }

    //旋转变换
    public void rotate(float angle, float x, float y, float z) {
        Matrix.rotateM(mMatrixTransform, 0, angle, x, y, z);
    }

    //缩放变换
    public void scale(float x, float y, float z) {
        Matrix.scaleM(mMatrixTransform, 0, x, y, z);
    }

    public void frustum(float left, float right, float bottom, float top, float near, float far) {
        Matrix.frustumM(mMatrixProjection, 0, left, right, bottom, top, near, far);
    }

    public void ortho(float left, float right, float bottom, float top, float near, float far) {
        Matrix.orthoM(mMatrixProjection, 0, left, right, bottom, top, near, far);
    }

    public void perspective(int offset, float fovy, float aspect, float zNear, float zFar) {
        Matrix.perspectiveM(mMatrixProjection, offset, fovy, aspect, zNear, zFar);
    }

    public float[] getFinalMatrix() {
        float[] ans = new float[16];
        Matrix.multiplyMM(ans, 0, mMatrixProjection, 0, mMatrixTransform, 0);
        return ans;
    }

    public static void logMatrix(float[] matrix) {
        logMatrix(TAG, matrix);
    }

    public static void logMatrix(String tag, float[] matrix) {
        logMatrix(tag, "", matrix);
    }

    public static void logMatrix(String tag, String name, float[] matrix) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (float f: matrix) {
            builder.append(f).append(",");
            if(i == 3) {
                builder.append("\n");
                i = 0;
                continue;
            }
            i++;
        }
        Log.e(tag, name + " matrix: \n" + builder.toString());
    }
}
