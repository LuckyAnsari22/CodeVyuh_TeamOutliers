package com.greeniq.app.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

/**
 * OpenGL ES 2.0 renderer — India's national highway network.
 * Port of the Three.js 3D road skeleton from epic_hero.html.
 *
 * Features:
 * - 7 NH routes as smooth tube geometry
 * - ~20 city nodes as animated glowing spheres
 * - Truck particles moving along routes
 * - CO₂ particle cloud (additive blending)
 * - State machine: Neutral → Diseased → GREENIQ Healed
 */
class RoadMapRenderer : GLSurfaceView.Renderer {

    // ── State machine ──
    enum class MapState { NEUTRAL, DISEASED, HEALING }
    var currentState = MapState.NEUTRAL
        set(value) {
            field = value
            stateTransitionStart = elapsedTime
        }

    // ── Matrices ──
    private val viewMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    // ── Timing ──
    private var startTime = 0L
    private var elapsedTime = 0f
    private var stateTransitionStart = 0f
    private var frameCount = 0
    private var lastFpsTime = 0L
    var currentFps = 60
        private set

    // ── Camera ──
    private var cameraY = 45f
    private var cameraZ = 35f
    private var targetCameraY = 45f
    private var targetCameraZ = 35f

    // ── Route data ──
    private data class CityCoord(val lat: Float, val lng: Float, val name: String)
    private data class RouteInfo(
        val name: String, val r: Float, val g: Float, val b: Float,
        val cities: List<CityCoord>
    )

    private val routes = listOf(
        RouteInfo("NH-48", 0.23f, 0.51f, 0.96f, listOf(
            CityCoord(28.704f, 77.102f, "Delhi"),
            CityCoord(28.459f, 77.026f, "Gurugram"),
            CityCoord(26.912f, 75.787f, "Jaipur"),
            CityCoord(22.307f, 73.181f, "Vadodara"),
            CityCoord(19.076f, 72.877f, "Mumbai")
        )),
        RouteInfo("NH-44", 0.55f, 0.36f, 0.96f, listOf(
            CityCoord(34.083f, 74.797f, "Srinagar"),
            CityCoord(28.704f, 77.102f, "Delhi"),
            CityCoord(23.259f, 77.412f, "Bhopal"),
            CityCoord(17.385f, 78.486f, "Hyderabad"),
            CityCoord(13.082f, 80.273f, "Chennai"),
            CityCoord(8.077f, 77.550f, "Kanyakumari")
        )),
        RouteInfo("NH-16", 0.02f, 0.71f, 0.83f, listOf(
            CityCoord(22.572f, 88.362f, "Kolkata"),
            CityCoord(20.296f, 85.824f, "Bhubaneswar"),
            CityCoord(17.687f, 83.218f, "Visakhapatnam"),
            CityCoord(13.082f, 80.273f, "Chennai")
        )),
        RouteInfo("NH-19", 0.96f, 0.62f, 0.04f, listOf(
            CityCoord(28.704f, 77.102f, "Delhi"),
            CityCoord(27.177f, 78.007f, "Agra"),
            CityCoord(25.317f, 82.973f, "Varanasi"),
            CityCoord(22.572f, 88.362f, "Kolkata")
        )),
        RouteInfo("NH-65", 0.93f, 0.29f, 0.60f, listOf(
            CityCoord(18.520f, 73.856f, "Pune"),
            CityCoord(17.446f, 78.448f, "Hyderabad"),
            CityCoord(16.506f, 80.648f, "Vijayawada")
        )),
        RouteInfo("NH-52", 0.08f, 0.72f, 0.65f, listOf(
            CityCoord(26.912f, 75.787f, "Jaipur"),
            CityCoord(23.022f, 72.571f, "Ahmedabad"),
            CityCoord(21.170f, 72.831f, "Surat"),
            CityCoord(19.076f, 72.877f, "Mumbai")
        )),
        RouteInfo("NH-27", 0.39f, 0.45f, 0.55f, listOf(
            CityCoord(26.449f, 80.331f, "Kanpur"),
            CityCoord(26.846f, 80.946f, "Lucknow"),
            CityCoord(25.317f, 82.973f, "Varanasi")
        ))
    )

    // ── Unique cities ──
    private val uniqueCities: List<CityCoord> by lazy {
        val seen = mutableSetOf<String>()
        routes.flatMap { it.cities }.filter { seen.add(it.name) }
    }

    // ── Shader programs ──
    private var lineProgram = 0
    private var pointProgram = 0

    // ── Geometry buffers ──
    private val routeBuffers = mutableListOf<RouteBuffer>()
    private val cityBuffers = mutableListOf<CityBuffer>()
    private var starBuffer: FloatBuffer? = null
    private var starCount = 0
    private var gridBuffer: FloatBuffer? = null
    private var gridVertexCount = 0

    // ── Truck particles ──
    private val truckParticles = mutableListOf<TruckParticle>()
    private var truckBuffer: FloatBuffer? = null
    private var truckColorBuffer: FloatBuffer? = null

    private data class RouteBuffer(val vertexBuffer: FloatBuffer, val vertexCount: Int,
                                    val r: Float, val g: Float, val b: Float)
    private data class CityBuffer(val x: Float, val y: Float, val z: Float, val name: String)
    private data class TruckParticle(val routeIndex: Int, var progress: Float)

    // ── Coordinate mapping ──
    private fun latLngTo3D(lat: Float, lng: Float): FloatArray {
        val x = (lng - 80.0f) * 1.8f
        val z = -(lat - 22.0f) * 1.8f
        return floatArrayOf(x, 0f, z)
    }

    // ── Catmull-Rom interpolation ──
    private fun catmullRomPoint(p0: FloatArray, p1: FloatArray, p2: FloatArray, p3: FloatArray, t: Float): FloatArray {
        val t2 = t * t; val t3 = t2 * t
        return floatArrayOf(
            0.5f * ((2 * p1[0]) + (-p0[0] + p2[0]) * t + (2 * p0[0] - 5 * p1[0] + 4 * p2[0] - p3[0]) * t2 + (-p0[0] + 3 * p1[0] - 3 * p2[0] + p3[0]) * t3),
            0.5f * ((2 * p1[1]) + (-p0[1] + p2[1]) * t + (2 * p0[1] - 5 * p1[1] + 4 * p2[1] - p3[1]) * t2 + (-p0[1] + 3 * p1[1] - 3 * p2[1] + p3[1]) * t3),
            0.5f * ((2 * p1[2]) + (-p0[2] + p2[2]) * t + (2 * p0[2] - 5 * p1[2] + 4 * p2[2] - p3[2]) * t2 + (-p0[2] + 3 * p1[2] - 3 * p2[2] + p3[2]) * t3)
        )
    }

    // ── Generate smooth curve points ──
    private fun generateCurvePoints(cities: List<CityCoord>, segments: Int = 80): List<FloatArray> {
        val points3D = cities.map { latLngTo3D(it.lat, it.lng) }
        if (points3D.size < 2) return points3D
        val result = mutableListOf<FloatArray>()
        for (i in 0 until points3D.size - 1) {
            val p0 = points3D[maxOf(0, i - 1)]
            val p1 = points3D[i]
            val p2 = points3D[minOf(points3D.size - 1, i + 1)]
            val p3 = points3D[minOf(points3D.size - 1, i + 2)]
            val segsPerSpan = segments / (points3D.size - 1)
            for (j in 0 until segsPerSpan) {
                val t = j.toFloat() / segsPerSpan
                result.add(catmullRomPoint(p0, p1, p2, p3, t))
            }
        }
        result.add(points3D.last())
        return result
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.008f, 0.016f, 0.031f, 1f) // #020408
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glLineWidth(3f)

        startTime = System.nanoTime()
        lastFpsTime = System.currentTimeMillis()

        compileShaders()
        buildGeometry()
    }

    private fun compileShaders() {
        // Line/triangle shader
        val lineVert = """
            uniform mat4 uMVPMatrix;
            attribute vec4 aPosition;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
            }
        """.trimIndent()
        val lineFrag = """
            precision mediump float;
            uniform vec4 uColor;
            void main() {
                gl_FragColor = uColor;
            }
        """.trimIndent()
        lineProgram = createProgram(lineVert, lineFrag)

        // Point shader (for stars, trucks, cities)
        val pointVert = """
            uniform mat4 uMVPMatrix;
            attribute vec4 aPosition;
            uniform float uPointSize;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                gl_PointSize = uPointSize;
            }
        """.trimIndent()
        val pointFrag = """
            precision mediump float;
            uniform vec4 uColor;
            void main() {
                float dist = length(gl_PointCoord - vec2(0.5));
                if (dist > 0.5) discard;
                float alpha = smoothstep(0.5, 0.2, dist);
                gl_FragColor = vec4(uColor.rgb, uColor.a * alpha);
            }
        """.trimIndent()
        pointProgram = createProgram(pointVert, pointFrag)
    }

    private fun buildGeometry() {
        // ── Routes ──
        routes.forEach { route ->
            val curvePoints = generateCurvePoints(route.cities)
            val verts = FloatArray(curvePoints.size * 3)
            curvePoints.forEachIndexed { i, pt ->
                verts[i * 3] = pt[0]; verts[i * 3 + 1] = pt[1]; verts[i * 3 + 2] = pt[2]
            }
            routeBuffers.add(RouteBuffer(
                createFloatBuffer(verts), curvePoints.size,
                route.r, route.g, route.b
            ))
            // Add truck particles for this route
            for (j in 0 until 20) {
                truckParticles.add(TruckParticle(routeBuffers.size - 1, Math.random().toFloat()))
            }
        }

        // ── Cities ──
        uniqueCities.forEach { city ->
            val pos = latLngTo3D(city.lat, city.lng)
            cityBuffers.add(CityBuffer(pos[0], pos[1], pos[2], city.name))
        }

        // ── Stars ──
        starCount = 3000
        val starVerts = FloatArray(starCount * 3)
        for (i in 0 until starCount) {
            starVerts[i * 3] = (Math.random().toFloat() - 0.5f) * 400
            starVerts[i * 3 + 1] = Math.random().toFloat() * 150 + 20
            starVerts[i * 3 + 2] = (Math.random().toFloat() - 0.5f) * 400
        }
        starBuffer = createFloatBuffer(starVerts)

        // ── Grid ──
        val gridLines = mutableListOf<Float>()
        val gridSpan = 60f; val gridStep = 2f
        var x = -gridSpan; while (x <= gridSpan) {
            gridLines.addAll(listOf(x, -0.5f, -gridSpan, x, -0.5f, gridSpan)); x += gridStep
        }
        var z = -gridSpan; while (z <= gridSpan) {
            gridLines.addAll(listOf(-gridSpan, -0.5f, z, gridSpan, -0.5f, z)); z += gridStep
        }
        gridVertexCount = gridLines.size / 3
        gridBuffer = createFloatBuffer(gridLines.toFloatArray())
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height
        Matrix.perspectiveM(projMatrix, 0, 50f, ratio, 0.1f, 500f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        elapsedTime = (System.nanoTime() - startTime) / 1_000_000_000f

        // FPS counter
        frameCount++
        val now = System.currentTimeMillis()
        if (now - lastFpsTime >= 1000) {
            currentFps = frameCount
            frameCount = 0
            lastFpsTime = now
        }

        updateState()
        updateCamera()

        // View matrix
        Matrix.setLookAtM(viewMatrix, 0,
            0f, cameraY, cameraZ,
            0f, 0f, -2f,
            0f, 1f, 0f)

        drawStars()
        drawGrid()
        drawRoutes()
        drawTrucks()
        drawCities()
    }

    private fun updateState() {
        val transTime = elapsedTime - stateTransitionStart
        when (currentState) {
            MapState.NEUTRAL -> {
                targetCameraY = 45f; targetCameraZ = 35f
            }
            MapState.DISEASED -> {
                targetCameraY = 35f; targetCameraZ = 38f
            }
            MapState.HEALING -> {
                targetCameraY = 40f; targetCameraZ = 25f
            }
        }
    }

    private fun updateCamera() {
        cameraY += (targetCameraY - cameraY) * 0.03f
        cameraZ += (targetCameraZ - cameraZ) * 0.03f
    }

    private fun getRouteColor(baseR: Float, baseG: Float, baseB: Float): FloatArray {
        val t = minOf(1f, (elapsedTime - stateTransitionStart) / 2.5f)
        return when (currentState) {
            MapState.NEUTRAL -> floatArrayOf(baseR, baseG, baseB, 0.7f)
            MapState.DISEASED -> {
                val r = baseR + (0.94f - baseR) * t
                val g = baseG + (0.27f - baseG) * t
                val b = baseB + (0.27f - baseB) * t
                floatArrayOf(r, g, b, 0.8f)
            }
            MapState.HEALING -> {
                val r = baseR + (0.06f - baseR) * t
                val g = baseG + (0.73f - baseG) * t
                val b = baseB + (0.51f - baseB) * t
                floatArrayOf(r, g, b, 0.9f)
            }
        }
    }

    private fun drawStars() {
        starBuffer?.let { buf ->
            GLES20.glUseProgram(pointProgram)
            Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, viewMatrix, 0)
            GLES20.glUniformMatrix4fv(
                GLES20.glGetUniformLocation(pointProgram, "uMVPMatrix"), 1, false, mvpMatrix, 0)
            GLES20.glUniform4f(
                GLES20.glGetUniformLocation(pointProgram, "uColor"), 1f, 1f, 1f, 0.5f)
            GLES20.glUniform1f(
                GLES20.glGetUniformLocation(pointProgram, "uPointSize"), 2f)

            val posHandle = GLES20.glGetAttribLocation(pointProgram, "aPosition")
            GLES20.glEnableVertexAttribArray(posHandle)
            buf.position(0)
            GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 12, buf)
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, starCount)
            GLES20.glDisableVertexAttribArray(posHandle)
        }
    }

    private fun drawGrid() {
        gridBuffer?.let { buf ->
            GLES20.glUseProgram(lineProgram)
            Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, viewMatrix, 0)
            GLES20.glUniformMatrix4fv(
                GLES20.glGetUniformLocation(lineProgram, "uMVPMatrix"), 1, false, mvpMatrix, 0)
            GLES20.glUniform4f(
                GLES20.glGetUniformLocation(lineProgram, "uColor"), 0.04f, 0.09f, 0.16f, 0.12f)

            val posHandle = GLES20.glGetAttribLocation(lineProgram, "aPosition")
            GLES20.glEnableVertexAttribArray(posHandle)
            buf.position(0)
            GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 12, buf)
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, gridVertexCount)
            GLES20.glDisableVertexAttribArray(posHandle)
        }
    }

    private fun drawRoutes() {
        GLES20.glUseProgram(lineProgram)
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, viewMatrix, 0)
        GLES20.glUniformMatrix4fv(
            GLES20.glGetUniformLocation(lineProgram, "uMVPMatrix"), 1, false, mvpMatrix, 0)

        val posHandle = GLES20.glGetAttribLocation(lineProgram, "aPosition")
        val colorHandle = GLES20.glGetUniformLocation(lineProgram, "uColor")

        routeBuffers.forEach { rb ->
            val color = getRouteColor(rb.r, rb.g, rb.b)
            GLES20.glUniform4f(colorHandle, color[0], color[1], color[2], color[3])
            GLES20.glEnableVertexAttribArray(posHandle)
            rb.vertexBuffer.position(0)
            GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 12, rb.vertexBuffer)
            GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, rb.vertexCount)
            GLES20.glDisableVertexAttribArray(posHandle)
        }
    }

    private fun drawTrucks() {
        val speed = when (currentState) {
            MapState.NEUTRAL -> 0.004f
            MapState.DISEASED -> 0.001f
            MapState.HEALING -> 0.007f
        }

        // Update positions
        val positions = FloatArray(truckParticles.size * 3)
        truckParticles.forEachIndexed { i, truck ->
            truck.progress = (truck.progress + speed) % 1f
            val rb = routeBuffers[truck.routeIndex]
            val totalVerts = rb.vertexCount
            val idx = (truck.progress * (totalVerts - 1)).toInt().coerceIn(0, totalVerts - 1)
            rb.vertexBuffer.position(idx * 3)
            positions[i * 3] = rb.vertexBuffer.get()
            positions[i * 3 + 1] = rb.vertexBuffer.get() + 0.25f
            positions[i * 3 + 2] = rb.vertexBuffer.get()
        }

        val buf = createFloatBuffer(positions)
        GLES20.glUseProgram(pointProgram)
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, viewMatrix, 0)
        GLES20.glUniformMatrix4fv(
            GLES20.glGetUniformLocation(pointProgram, "uMVPMatrix"), 1, false, mvpMatrix, 0)

        val truckColor = when (currentState) {
            MapState.NEUTRAL -> floatArrayOf(1f, 1f, 1f, 0.85f)
            MapState.DISEASED -> floatArrayOf(0.94f, 0.27f, 0.27f, 0.85f)
            MapState.HEALING -> floatArrayOf(0.06f, 1f, 0.53f, 0.9f)
        }
        GLES20.glUniform4f(
            GLES20.glGetUniformLocation(pointProgram, "uColor"),
            truckColor[0], truckColor[1], truckColor[2], truckColor[3])
        GLES20.glUniform1f(
            GLES20.glGetUniformLocation(pointProgram, "uPointSize"), 6f)

        // Enable additive blending for trucks
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)

        val posHandle = GLES20.glGetAttribLocation(pointProgram, "aPosition")
        GLES20.glEnableVertexAttribArray(posHandle)
        buf.position(0)
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 12, buf)
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, truckParticles.size)
        GLES20.glDisableVertexAttribArray(posHandle)

        // Reset blending
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    private fun drawCities() {
        GLES20.glUseProgram(pointProgram)
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, viewMatrix, 0)
        GLES20.glUniformMatrix4fv(
            GLES20.glGetUniformLocation(pointProgram, "uMVPMatrix"), 1, false, mvpMatrix, 0)

        cityBuffers.forEachIndexed { idx, city ->
            val pulse = sin(elapsedTime * 2.5f + idx * 0.7f) * 0.5f + 0.5f
            val glowSize = 12f + pulse * 8f

            // Glow color based on state
            val color = when (currentState) {
                MapState.NEUTRAL -> floatArrayOf(0.23f, 0.51f, 0.96f, 0.35f)
                MapState.DISEASED -> floatArrayOf(0.94f, 0.27f, 0.27f, 0.4f)
                MapState.HEALING -> floatArrayOf(0.06f, 1f, 0.53f, 0.5f)
            }

            GLES20.glUniform4f(
                GLES20.glGetUniformLocation(pointProgram, "uColor"),
                color[0], color[1], color[2], color[3])
            GLES20.glUniform1f(
                GLES20.glGetUniformLocation(pointProgram, "uPointSize"), glowSize)

            // Enable additive blending for glow
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)

            val buf = createFloatBuffer(floatArrayOf(city.x, city.y, city.z))
            val posHandle = GLES20.glGetAttribLocation(pointProgram, "aPosition")
            GLES20.glEnableVertexAttribArray(posHandle)
            buf.position(0)
            GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 12, buf)
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1)

            // Draw inner core (bright white)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            val coreColor = when (currentState) {
                MapState.HEALING -> floatArrayOf(0f, 1f, 0.53f, 0.95f)
                else -> floatArrayOf(1f, 1f, 1f, 0.9f)
            }
            GLES20.glUniform4f(
                GLES20.glGetUniformLocation(pointProgram, "uColor"),
                coreColor[0], coreColor[1], coreColor[2], coreColor[3])
            GLES20.glUniform1f(
                GLES20.glGetUniformLocation(pointProgram, "uPointSize"), 5f)
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1)

            GLES20.glDisableVertexAttribArray(posHandle)
        }
    }

    // ── Helpers ──
    private fun createFloatBuffer(data: FloatArray): FloatBuffer {
        return ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(data); position(0) }
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertShader)
        GLES20.glAttachShader(program, fragShader)
        GLES20.glLinkProgram(program)
        return program
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}
