package com.example.demo

import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.opengl.*
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.*
import javax.microedition.khronos.egl.EGL10

object GlUtil {

    private const val TAG = "GlUtil"

    private val TEXTURE_2D_HEADER_3 = StringBuilder().apply {
        append("#version 300 es \n")
        append("precision highp float;\n")
        append("uniform sampler2D texture_0;\n")
    }.toString()

    private val TEXTURE_OES_HEADER_3 = StringBuilder().apply {
        append("#version 300 es \n")
        append("#extension GL_OES_EGL_image_external_essl3 : require \n")
        append("precision highp float;\n")
        append("uniform samplerExternalOES texture_0;\n")
    }.toString()

    /** Thrown when an OpenGL error occurs and [.glAssertionsEnabled] is `true`.  */
    class GlException
    /** Creates an instance with the specified error message.  */
        (message: String?) : RuntimeException(message)
    
    /** Whether to throw a [GlException] in case of an OpenGL error.  */
    private var glAssertionsEnabled = false

    /** Number of elements in a 3d homogeneous coordinate vector describing a vertex.  */
    private const val HOMOGENEOUS_COORDINATE_VECTOR_SIZE = 4

    /** Length of the normalized device coordinate (NDC) space, which spans from -1 to 1.  */
    const val LENGTH_NDC = 2f

    // https://www.khronos.org/registry/EGL/extensions/EXT/EGL_EXT_protected_content.txt
    private const val EXTENSION_PROTECTED_CONTENT = "EGL_EXT_protected_content"

    // https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_surfaceless_context.txt
    private const val EXTENSION_SURFACELESS_CONTEXT = "EGL_KHR_surfaceless_context"

    // https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_gl_colorspace.txt
    private const val EGL_GL_COLORSPACE_KHR = 0x309D

    // https://www.khronos.org/registry/EGL/extensions/EXT/EGL_EXT_gl_colorspace_bt2020_linear.txt
    private const val EGL_GL_COLORSPACE_BT2020_PQ_EXT = 0x3340

    private val EGL_WINDOW_SURFACE_ATTRIBUTES_NONE = intArrayOf(EGL14.EGL_NONE)
    private val EGL_WINDOW_SURFACE_ATTRIBUTES_BT2020_PQ =
        intArrayOf(EGL_GL_COLORSPACE_KHR, EGL_GL_COLORSPACE_BT2020_PQ_EXT, EGL14.EGL_NONE)
    private val EGL_CONFIG_ATTRIBUTES_RGBA_8888 = intArrayOf(
        EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
        EGL14.EGL_RED_SIZE,  /* redSize= */8,
        EGL14.EGL_GREEN_SIZE,  /* greenSize= */8,
        EGL14.EGL_BLUE_SIZE,  /* blueSize= */8,
        EGL14.EGL_ALPHA_SIZE,  /* alphaSize= */8,
        EGL14.EGL_DEPTH_SIZE,  /* depthSize= */0,
        EGL14.EGL_STENCIL_SIZE,  /* stencilSize= */0,
        EGL14.EGL_NONE
    )
    private val EGL_CONFIG_ATTRIBUTES_RGBA_1010102 = intArrayOf(
        EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
        EGL14.EGL_RED_SIZE,  /* redSize= */10,
        EGL14.EGL_GREEN_SIZE,  /* greenSize= */10,
        EGL14.EGL_BLUE_SIZE,  /* blueSize= */10,
        EGL14.EGL_ALPHA_SIZE,  /* alphaSize= */2,
        EGL14.EGL_DEPTH_SIZE,  /* depthSize= */0,
        EGL14.EGL_STENCIL_SIZE,  /* stencilSize= */0,
        EGL14.EGL_NONE
    )

    /** Bounds of normalized device coordinates, commonly used for defining viewport boundaries.  */
    fun getNormalizedCoordinateBounds(): FloatArray {
        return floatArrayOf(
            -1f, -1f, 0f, 1f, 
            1f, -1f, 0f, 1f, 
            -1f, 1f, 0f, 1f, 
            1f, 1f, 0f, 1f)
    }

    /** Typical bounds used for sampling from textures.  */
    fun getTextureCoordinateBounds(): FloatArray {
        return floatArrayOf(
            0f, 0f, 0f, 1f, 
            1f, 0f, 0f, 1f, 
            0f, 1f, 0f, 1f, 
            1f, 1f, 0f, 1f)
    }

    /** Flattens the list of 4 element NDC coordinate vectors into a buffer.  */
    fun createVertexBuffer(vertexList: List<FloatArray>): FloatArray {
        val vertexBuffer = FloatArray(HOMOGENEOUS_COORDINATE_VECTOR_SIZE * vertexList.size)
        for (i in vertexList.indices) {
            vertexList[i].let {
                System.arraycopy( /* src= */
                    it,  /* srcPos= */
                    0,  /* dest= */
                    vertexBuffer,  /* destPos= */
                    HOMOGENEOUS_COORDINATE_VECTOR_SIZE * i,  /* length= */
                    HOMOGENEOUS_COORDINATE_VECTOR_SIZE
                )
            }
        }
        return vertexBuffer
    }

    /**
     * Returns whether the {@value #EXTENSION_SURFACELESS_CONTEXT} extension is supported.
     *
     *
     * This extension allows passing [EGL14.EGL_NO_SURFACE] for both the write and read
     * surfaces in a call to [EGL14.eglMakeCurrent].
     */
    fun isSurfacelessContextExtensionSupported(): Boolean {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val eglExtensions = EGL14.eglQueryString(display, EGL10.EGL_EXTENSIONS)
        return eglExtensions != null && eglExtensions.contains(EXTENSION_SURFACELESS_CONTEXT)
    }

    /** Returns an initialized default [EGLDisplay].  */
    fun createEglDisplay(): EGLDisplay {
        return Api17.createEglDisplay()
    }

    /** Returns a new [EGLContext] for the specified [EGLDisplay].  */
    fun createEglContext(eglDisplay: EGLDisplay?): EGLContext? {
        return Api17.createEglContext(eglDisplay,  /* version= */2, EGL_CONFIG_ATTRIBUTES_RGBA_8888)
    }

    /**
     * Returns a new [EGLContext] for the specified [EGLDisplay], requesting ES 3 and an
     * RGBA 1010102 config.
     */
    fun createEglContextEs3Rgba1010102(eglDisplay: EGLDisplay?): EGLContext? {
        return Api17.createEglContext(
            eglDisplay,  /* version= */
            3,
            EGL_CONFIG_ATTRIBUTES_RGBA_1010102
        )
    }

    /**
     * Returns a new [EGLSurface] wrapping the specified `surface`.
     *
     * @param eglDisplay The [EGLDisplay] to attach the surface to.
     * @param surface The surface to wrap; must be a surface, surface texture or surface holder.
     */
    fun getEglSurface(eglDisplay: EGLDisplay?, surface: Any?): EGLSurface? {
        return Api17.getEglSurface(
            eglDisplay, surface, EGL_CONFIG_ATTRIBUTES_RGBA_8888, EGL_WINDOW_SURFACE_ATTRIBUTES_NONE
        )
    }

    /**
     * Returns a new [EGLSurface] wrapping the specified `surface`, for HDR rendering with
     * Rec. 2020 color primaries and using the PQ transfer function.
     *
     * @param eglDisplay The [EGLDisplay] to attach the surface to.
     * @param surface The surface to wrap; must be a surface, surface texture or surface holder.
     */
    fun getEglSurfaceBt2020Pq(eglDisplay: EGLDisplay?, surface: Any?): EGLSurface? {
        return Api17.getEglSurface(
            eglDisplay,
            surface,
            EGL_CONFIG_ATTRIBUTES_RGBA_1010102,
            EGL_WINDOW_SURFACE_ATTRIBUTES_BT2020_PQ
        )
    }

    /**
     * Creates a new [EGLSurface] wrapping a pixel buffer.
     *
     * @param eglDisplay The [EGLDisplay] to attach the surface to.
     * @param width The width of the pixel buffer.
     * @param height The height of the pixel buffer.
     */
    private fun createPbufferSurface(eglDisplay: EGLDisplay, width: Int, height: Int): EGLSurface {
        val pbufferAttributes = intArrayOf(
            EGL14.EGL_WIDTH, width,
            EGL14.EGL_HEIGHT, height,
            EGL14.EGL_NONE
        )
        return Api17.createEglPbufferSurface(
            eglDisplay, EGL_CONFIG_ATTRIBUTES_RGBA_8888, pbufferAttributes
        )
    }

    /**
     * Returns a placeholder [EGLSurface] to use when reading and writing to the surface is not
     * required.
     *
     * @param eglDisplay The [EGLDisplay] to attach the surface to.
     * @return [EGL14.EGL_NO_SURFACE] if supported and a 1x1 pixel buffer surface otherwise.
     */
    fun createPlaceholderEglSurface(eglDisplay: EGLDisplay): EGLSurface? {
        return if (isSurfacelessContextExtensionSupported()) EGL14.EGL_NO_SURFACE else createPbufferSurface(
            eglDisplay,  /* width= */1,  /* height= */ 1
        )
    }

    /**
     * Creates and focuses a new [EGLSurface] wrapping a 1x1 pixel buffer.
     *
     * @param eglContext The [EGLContext] to make current.
     * @param eglDisplay The [EGLDisplay] to attach the surface to.
     */
    fun focusPlaceholderEglSurface(eglContext: EGLContext?, eglDisplay: EGLDisplay) {
        val eglSurface = createPbufferSurface(eglDisplay,  /* width= */1,  /* height= */1)
        focusEglSurface(eglDisplay, eglContext, eglSurface,  /* width= */1,  /* height= */1)
    }

    /**
     * Creates and focuses a new [EGLSurface] wrapping a 1x1 pixel buffer, for HDR rendering
     * with Rec. 2020 color primaries and using the PQ transfer function.
     *
     * @param eglContext The [EGLContext] to make current.
     * @param eglDisplay The [EGLDisplay] to attach the surface to.
     */
    fun focusPlaceholderEglSurfaceBt2020Pq(
        eglContext: EGLContext?, eglDisplay: EGLDisplay?
    ) {
        val pbufferAttributes = intArrayOf(
            EGL14.EGL_WIDTH,  /* width= */ 1,
            EGL14.EGL_HEIGHT,  /* height= */ 1,
            EGL_GL_COLORSPACE_KHR,
            EGL_GL_COLORSPACE_BT2020_PQ_EXT,
            EGL14.EGL_NONE
        )
        val eglSurface = Api17.createEglPbufferSurface(
            eglDisplay, EGL_CONFIG_ATTRIBUTES_RGBA_1010102, pbufferAttributes
        )
        focusEglSurface(eglDisplay, eglContext, eglSurface,  /* width= */1,  /* height= */1)
    }

    /**
     * If there is an OpenGl error, logs the error and if [.glAssertionsEnabled] is true throws
     * a [GlException].
     */
    fun checkGlError(method:String) {
        var lastError = GLES30.GL_NO_ERROR
        var error: Int
        while (GLES30.glGetError().also { error = it } != GLES30.GL_NO_ERROR) {
            Log.e(TAG, "$method glError: " + GLU.gluErrorString(error))
            lastError = error
        }
        if (lastError != GLES30.GL_NO_ERROR) {
            throwGlException("$method glError: " + GLU.gluErrorString(lastError))
        }
    }

    /**
     * Asserts the texture size is valid.
     *
     * @param width The width for a texture.
     * @param height The height for a texture.
     * @throws GlException If the texture width or height is invalid.
     */
    fun assertValidTextureSize(width: Int, height: Int) {

        // For valid GL sizes, see:
        // https://www.khronos.org/registry/OpenGL-Refpages/es2.0/xhtml/glTexImage2D.xml
        val maxTextureSizeBuffer = IntArray(1)
        GLES30.glGetIntegerv(GLES30.GL_MAX_TEXTURE_SIZE, maxTextureSizeBuffer, 0)
        val maxTextureSize = maxTextureSizeBuffer[0]
        if (width < 0 || height < 0) {
            throwGlException("width or height is less than 0")
        }
        if (width > maxTextureSize || height > maxTextureSize) {
            throwGlException("width or height is greater than GL_MAX_TEXTURE_SIZE $maxTextureSize")
        }
    }

    /**
     * Makes the specified `eglSurface` the render target, using a viewport of `width` by
     * `height` pixels.
     */
    fun focusEglSurface(
        eglDisplay: EGLDisplay?,
        eglContext: EGLContext?,
        eglSurface: EGLSurface?,
        width: Int,
        height: Int
    ) {
        Api17.focusRenderTarget(
            eglDisplay, eglContext, eglSurface,  /* framebuffer= */0, width, height
        )
    }

    /**
     * Makes the specified `framebuffer` the render target, using a viewport of `width` by
     * `height` pixels.
     */
    fun focusFramebuffer(
        eglDisplay: EGLDisplay?,
        eglContext: EGLContext?,
        eglSurface: EGLSurface?,
        framebuffer: Int,
        width: Int,
        height: Int
    ) {
        Api17.focusRenderTarget(eglDisplay, eglContext, eglSurface, framebuffer, width, height)
    }

    /**
     * Deletes a GL texture.
     *
     * @param textureId The ID of the texture to delete.
     */
    fun deleteTexture(textureId: Int) {
        GLES30.glDeleteTextures( /* n= */1, intArrayOf(textureId),  /* offset= */0)
        checkGlError("glDeleteTextures")
    }

    /**
     * Destroys the [EGLContext] identified by the provided [EGLDisplay] and [ ].
     */
    fun destroyEglContext(
        eglDisplay: EGLDisplay?, eglContext: EGLContext?
    ) {
        Api17.destroyEglContext(eglDisplay, eglContext)
    }

    /**
     * Allocates a FloatBuffer with the given data.
     *
     * @param data Used to initialize the new buffer.
     */
    fun createBuffer(data: FloatArray): FloatBuffer? {
        return createBuffer(data.size).put(data).flip() as FloatBuffer
    }

    /**
     * Allocates a FloatBuffer.
     *
     * @param capacity The new buffer's capacity, in floats.
     */
    fun createBuffer(capacity: Int): FloatBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(capacity * 4)
        return byteBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer()
    }

    /**
     *
     * @param resourceId
     * @return
     */
    fun readRawResource(context: Context, resourceId: Int): String {
        val builder = StringBuilder()
        try {
            context.resources?.openRawResource(resourceId).also {
                    input ->
                val streamReader = InputStreamReader(input)
                val bufferedReader = BufferedReader(streamReader)
                var textLine: String?
                while (bufferedReader.readLine().also { textLine = it } != null) {
                    builder.append(textLine).append("\n")
                }
                bufferedReader.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Resources.NotFoundException) {
            e.printStackTrace()
        }
        return builder.toString()
    }

    fun createProgram(context: Context, vertexResId: Int, fragmentResId: Int, isOES: Boolean = true): Int {
        val textureHeader = if(isOES) TEXTURE_OES_HEADER_3 else TEXTURE_2D_HEADER_3
        return createProgram(
            readRawResource(context,vertexResId),
            textureHeader + readRawResource(context,fragmentResId)
        )
    }

    fun createProgram(vertexSource: String, fragmentSource: String): Int {
        // load vertex shader
        val vertexShader: Int = loadShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) {
            return 0
        }

        // load fragment shader
        val fragmentShader: Int = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        if (fragmentShader == 0) {
            return 0
        }

        // create program
        var program = GLES30.glCreateProgram()
        if (program != 0) {
            GLES30.glAttachShader(program, vertexShader)
            checkGlError("glAttachShader")
            GLES30.glAttachShader(program, fragmentShader)
            checkGlError("glAttachShader")
            GLES30.glLinkProgram(program)
            val linkStatus = IntArray(1)
            GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES30.GL_TRUE) {
                Log.e("ES20_ERROR", "Could not link program: ")
                Log.e("ES20_ERROR", GLES30.glGetProgramInfoLog(program))
                GLES30.glDeleteProgram(program)
                program = 0
            }
        }
        return program
    }

    /**
     * Converts a raw text file, saved as a resource, into an OpenGL ES shader.
     *
     * @param type  The warping of shader we will be creating.
     * @param resId The resource ID of the raw text file about to be turned into a shader.
     * @return The shader object handler.
     */
    fun loadShader(context: Context, type: Int, resId: Int): Int {
        val code: String = readRawResource(context,resId)
        return loadShader(type, code)
    }

    fun loadShader(type: Int, code: String?): Int {
        var shader = GLES30.glCreateShader(type)
        Log.e(TAG, "loadShader shader: $shader,code:\n $code")
        GLES30.glShaderSource(shader, code)
        GLES30.glCompileShader(shader)

        // Get the compilation status.
        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Error compiling shader: " + GLES30.glGetShaderInfoLog(shader))
            GLES30.glDeleteShader(shader)
            shader = 0
        }
        if (shader == 0) {
            throw java.lang.RuntimeException("Error creating shader.")
        }
        return shader
    }

    /**
     * Creates a GL_TEXTURE_EXTERNAL_OES with default configuration of GL_LINEAR filtering and
     * GL_CLAMP_TO_EDGE wrapping.
     */
    fun createExternalTexture(): Int {
        val texId = generateTexture()
        bindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId)
        return texId
    }

    /**
     * Returns the texture identifier for a newly-allocated texture with the specified dimensions.
     *
     * @param textureTarget The target to which the texture is bound, e.g. [     ][GLES30.GL_TEXTURE_2D] for a two-dimensional texture or [     ][GLES11Ext.GL_TEXTURE_EXTERNAL_OES] for an external texture.
     */
    fun createTexture(textureTarget: Int): Int {
        val texId = generateTexture()
        bindTexture(textureTarget, texId)
        checkGlError("bindTexture")
        return texId
    }

    /**
     * Returns the texture identifier for a newly-allocated texture with the specified dimensions.
     *
     * @param width of the new texture in pixels
     * @param height of the new texture in pixels
     */
    fun createTexture(width: Int, height: Int): Int {
        assertValidTextureSize(width, height)
        val texId = generateTexture()
        bindTexture(GLES30.GL_TEXTURE_2D, texId)
        val byteBuffer = ByteBuffer.allocateDirect(width * height * 4)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D,  /* level= */
            0,
            GLES30.GL_RGBA,
            width,
            height,  /* border= */
            0,
            GLES30.GL_RGBA,
            GLES30.GL_UNSIGNED_BYTE,
            byteBuffer
        )
        checkGlError("createTexture")
        return texId
    }

    /** Returns a new GL texture identifier.  */
    private fun generateTexture(): Int {
        checkEglException(
            !Objects.equals(EGL14.eglGetCurrentContext(), EGL14.EGL_NO_CONTEXT), "No current context"
        )
        val texId = IntArray(1)
        GLES30.glGenTextures( /* n= */1, texId,  /* offset= */0)
        checkGlError("glGenTextures")
        return texId[0]
    }

    fun genFrameBuffer(
        frameBufferId: IntArray, frameTextureId: IntArray, width: Int, height: Int,
        internalFormat: Int, format: Int, type: Int
    ) {
        Log.d(TAG, "genFrameBuffer called type = $type")
        //创建一个纹理，并设置纹理的环绕方式
        GLES30.glGenTextures(1, frameTextureId, 0)
        // create buffers
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, frameTextureId[0])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)

        //创建并Bind FBO，
        GLES30.glGenFramebuffers(1, frameBufferId, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBufferId[0])
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, frameTextureId[0])
        // specify texture as color attachment
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D,
            frameTextureId[0], 0
        )
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, null)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
    }

    /**
     * Binds the texture of the given type with default configuration of GL_LINEAR filtering and
     * GL_CLAMP_TO_EDGE wrapping.
     *
     * @param texId The texture identifier.
     * @param textureTarget The target to which the texture is bound, e.g. [     ][GLES30.GL_TEXTURE_2D] for a two-dimensional texture or [     ][GLES11Ext.GL_TEXTURE_EXTERNAL_OES] for an external texture.
     */
    fun bindTexture(textureTarget: Int, texId: Int) {
        GLES30.glBindTexture(textureTarget, texId)
        checkGlError("glBindTexture")
        GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        checkGlError("glTexParameteri")
    }

    /**
     * Returns a new framebuffer for the texture.
     *
     * @param texId The identifier of the texture to attach to the framebuffer.
     */
    fun createFboForTexture(texId: Int): Int {
        checkEglException(
            !Objects.equals(EGL14.eglGetCurrentContext(), EGL14.EGL_NO_CONTEXT), "No current context"
        )
        val fboId = IntArray(1)
        GLES30.glGenFramebuffers( /* n= */1, fboId,  /* offset= */0)
        checkGlError("glGenFramebuffers")
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboId[0])
        checkGlError("glBindFramebuffer")
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, texId, 0)
        checkGlError("glFramebufferTexture2D")
        return fboId[0]
    }

    fun loadTexture(context: Context, resourceId: Int): Int {
        val options = BitmapFactory.Options()
        options.inScaled = false // No pre-scaling

        // Read in the resource
        val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)

        val textureId = generateTexture()

        // Bind to the texture in OpenGL
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)

        // Set filtering

        // Set filtering
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_MIN_FILTER,
            GLES30.GL_LINEAR
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_MAG_FILTER,
            GLES30.GL_LINEAR
        )

        GLES30.glTexParameterf(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_WRAP_S,
            GLES30.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES30.glTexParameterf(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_WRAP_T,
            GLES30.GL_CLAMP_TO_EDGE.toFloat()
        )

        // Load the bitmap into the bound texture.
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        return textureId
    }

    /* package */
    fun throwGlException(errorMsg: String) {
        if (glAssertionsEnabled) {
            throw GlException(errorMsg)
        } else {
            Log.e(TAG, errorMsg)
        }
    }

    private fun checkEglException(expression: Boolean, errorMessage: String) {
        if (!expression) {
            throwGlException(errorMessage)
        }
    }

    private fun checkEglException(errorMessage: String) {
        val error = EGL14.eglGetError()
        checkEglException(error == EGL14.EGL_SUCCESS, "$errorMessage, error code: $error")
    }

    private object Api17 {
        fun createEglDisplay(): EGLDisplay {
            val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            checkEglException(eglDisplay != EGL14.EGL_NO_DISPLAY, "No EGL display.")
            if (!EGL14.eglInitialize(
                    eglDisplay, IntArray(1),  /* majorOffset= */
                    0, IntArray(1),  /* minorOffset= */
                    0
                )
            ) {
                throwGlException("Error in eglInitialize.")
            }
            checkGlError("createEglDisplay")
            return eglDisplay
        }

        fun createEglContext(
            eglDisplay: EGLDisplay?, version: Int, configAttributes: IntArray
        ): EGLContext? {
            val contextAttributes =
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, version, EGL14.EGL_NONE)
            val eglContext = EGL14.eglCreateContext(
                eglDisplay,
                getEglConfig(eglDisplay, configAttributes),
                EGL14.EGL_NO_CONTEXT,
                contextAttributes,  /* offset= */
                0
            )
            if (eglContext == null) {
                EGL14.eglTerminate(eglDisplay)
                throwGlException(
                    "eglCreateContext() failed to create a valid context. The device may not support EGL"
                            + " version "
                            + version
                )
            }
            checkGlError("createEglContext")
            return eglContext
        }

        fun getEglSurface(
            eglDisplay: EGLDisplay?,
            surface: Any?,
            configAttributes: IntArray,
            windowSurfaceAttributes: IntArray?
        ): EGLSurface {
            val eglSurface = EGL14.eglCreateWindowSurface(
                eglDisplay,
                getEglConfig(eglDisplay, configAttributes),
                surface,
                windowSurfaceAttributes,  /* offset= */
                0
            )
            checkEglException("Error creating surface")
            return eglSurface
        }

        fun createEglPbufferSurface(
            eglDisplay: EGLDisplay?, configAttributes: IntArray, pbufferAttributes: IntArray?
        ): EGLSurface {
            val eglSurface = EGL14.eglCreatePbufferSurface(
                eglDisplay,
                getEglConfig(eglDisplay, configAttributes),
                pbufferAttributes,  /* offset= */
                0
            )
            checkEglException("Error creating surface")
            return eglSurface
        }

        fun focusRenderTarget(
            eglDisplay: EGLDisplay?,
            eglContext: EGLContext?,
            eglSurface: EGLSurface?,
            framebuffer: Int,
            width: Int,
            height: Int
        ) {
            val boundFramebuffer = IntArray(1)
            GLES30.glGetIntegerv(GLES30.GL_FRAMEBUFFER_BINDING, boundFramebuffer,  /* offset= */0)
            if (boundFramebuffer[0] != framebuffer) {
                GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebuffer)
            }
            checkGlError("focusRenderTarget")
            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
            checkEglException("Error making context current")
            GLES30.glViewport( /* x= */0,  /* y= */0, width, height)
            checkGlError("glViewport")
        }

        fun destroyEglContext(
            eglDisplay: EGLDisplay?, eglContext: EGLContext?
        ) {
            if (eglDisplay == null) {
                return
            }
            EGL14.eglMakeCurrent(
                eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT
            )
            checkEglException("Error releasing context")
            if (eglContext != null) {
                EGL14.eglDestroyContext(eglDisplay, eglContext)
                checkEglException("Error destroying context")
            }
            EGL14.eglReleaseThread()
            checkEglException("Error releasing thread")
            EGL14.eglTerminate(eglDisplay)
            checkEglException("Error terminating display")
        }

        private fun getEglConfig(eglDisplay: EGLDisplay?, attributes: IntArray): EGLConfig? {
            val eglConfigs = arrayOfNulls<EGLConfig>(1)
            if (!EGL14.eglChooseConfig(
                    eglDisplay,
                    attributes,  /* attrib_listOffset= */
                    0,
                    eglConfigs,  /* configsOffset= */
                    0,  /* config_size= */
                    1, IntArray(1),  /* num_configOffset= */
                    0
                )
            ) {
                throwGlException("eglChooseConfig failed.")
            }
            return eglConfigs[0]
        }
    }
}