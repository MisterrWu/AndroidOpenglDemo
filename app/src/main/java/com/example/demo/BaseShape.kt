package com.example.demo

import android.content.Context
import com.example.demo.IShape

abstract class BaseShape(protected val context: Context) : IShape {

    @Volatile
    private var initialized:Boolean = false

    final override fun init() {
        if(!initialized) {
            initialize()
            initialized = true
        }
    }

    abstract fun initialize()

    override fun isInitialized(): Boolean {
        return initialized
    }
}