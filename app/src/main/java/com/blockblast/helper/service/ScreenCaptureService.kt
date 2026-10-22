package com.blockblast.helper.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.blockblast.helper.R
import com.blockblast.helper.solver.BlockSolver
import com.blockblast.helper.vision.BoardDetector
import kotlinx.coroutines.*

/**
 * Foreground Service ที่จับภาพหน้าจออย่างต่อเนื่อง
 */
class ScreenCaptureService : Service() {

    companion object {
        const val ACTION_START = "com.blockblast.helper.START_CAPTURE"
        const val ACTION_STOP = "com.blockblast.helper.STOP_CAPTURE"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "data"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "screen_capture_channel"

        var latestSuggestion: String = "รอการวิเคราะห์..."
            private set
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    private val detector = BoardDetector()
    private val solver = BlockSolver()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var isCapturing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        handlerThread = HandlerThread("ScreenCapture").also { it.start() }
        handler = Handler(handlerThread!!.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_DATA)
                }
                if (data != null) {
                    startCapture(resultCode, data)
                }
            }
            ACTION_STOP -> stopCapture()
        }
        return START_STICKY
    }

    private fun startCapture(resultCode: Int, data: Intent) {
        if (isCapturing) return

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpm.getMediaProjection(resultCode, data)

        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                stopCapture()
            }
        }, handler)

        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "BlockBlastCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, handler
        )

        isCapturing = true
        startAnalysisLoop()
        Log.d("ScreenCapture", "Capture started")
    }

    private fun startAnalysisLoop() {
        scope.launch {
            while (isCapturing) {
                try {
                    val bitmap = captureBitmap()
                    if (bitmap != null) {
                        analyzeFrame(bitmap)
                        bitmap.recycle()
                    }
                } catch (e: Exception) {
                    Log.e("ScreenCapture", "Analysis error", e)
                }
                delay(1500) // วิเคราะห์ทุก 1.5 วินาที
            }
        }
    }

    private fun captureBitmap(): Bitmap? {
        val image = imageReader?.acquireLatestImage() ?: return null
        return try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
        } finally {
            image.close()
        }
    }

    private fun analyzeFrame(bitmap: Bitmap) {
        val result = detector.detect(bitmap)
        if (result.confidence < 0.3f) {
            // ยังเป็น mock → ใช้ข้อความตัวอย่าง
            latestSuggestion = """
                🔍 กำลังวิเคราะห์หน้าจอ...
                
                (เวอร์ชันนี้ยังใช้ mock detection)
                กรุณาพัฒนา BoardDetector ให้จดจำกริดจริง
                
                คำแนะนำทั่วไป:
                • วางชิ้นใหญ่ก่อน
                • รักษาพื้นที่ 3×3
                • เคลียร์อย่างน้อย 1 เส้นต่อชุด
            """.trimIndent()
            return
        }

        val suggestion = solver.findBestSequence(
            com.blockblast.helper.data.GameState(result.board, result.pieces)
        )

        latestSuggestion = buildString {
            appendLine("✅ คำแนะนำที่ดีที่สุด")
            appendLine()
            suggestion.placements.forEachIndexed { i, p ->
                val colName = ('A' + p.col)
                appendLine("${i + 1}. ชิ้น #${p.shapeIndex + 1} → แถว ${p.row + 1}, คอลัมน์ $colName")
                if (p.linesCleared > 0) appendLine("   เคลียร์ ${p.linesCleared} เส้น")
            }
            appendLine()
            append(suggestion.reason)
        }

        // ส่งต่อไปยัง Overlay
        val intent = Intent("com.blockblast.helper.UPDATE_SUGGESTION")
        intent.putExtra("text", latestSuggestion)
        sendBroadcast(intent)
    }

    private fun stopCapture() {
        isCapturing = false
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d("ScreenCapture", "Capture stopped")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopCapture()
        scope.cancel()
        handlerThread?.quitSafely()
        super.onDestroy()
    }
}
