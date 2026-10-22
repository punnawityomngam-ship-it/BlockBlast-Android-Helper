package com.blockblast.helper

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.blockblast.helper.service.OverlayService
import com.blockblast.helper.service.ScreenCaptureService

class MainActivity : ComponentActivity() {

    private var isCapturing by mutableStateOf(false)

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_START
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_DATA, result.data)
            }
            ContextCompat.startForegroundService(this, serviceIntent)

            // เริ่ม Overlay ด้วย
            startService(Intent(this, OverlayService::class.java))
            isCapturing = true
            Toast.makeText(this, "เริ่มจับภาพหน้าจอแล้ว", Toast.LENGTH_SHORT).show()
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "อนุญาต Floating Window แล้ว", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BlockBlastHelperTheme {
                MainScreen(
                    isCapturing = isCapturing,
                    onStart = { startCaptureFlow() },
                    onStop = { stopCapture() },
                    onRequestOverlay = { requestOverlayPermission() }
                )
            }
        }
    }

    private fun startCaptureFlow() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "กรุณาเปิดสิทธิ์ Floating Window ก่อน", Toast.LENGTH_LONG).show()
            requestOverlayPermission()
            return
        }

        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(mpm.createScreenCaptureIntent())
    }

    private fun stopCapture() {
        val stopIntent = Intent(this, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_STOP
        }
        startService(stopIntent)
        stopService(Intent(this, OverlayService::class.java))
        isCapturing = false
        Toast.makeText(this, "หยุดจับภาพแล้ว", Toast.LENGTH_SHORT).show()
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }
}

@Composable
fun MainScreen(
    isCapturing: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRequestOverlay: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        Text(
            text = "Block Blast Helper",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00D4FF)
        )
        Text(
            text = "ตัวช่วยวางบล็อก + คอมโบ",
            fontSize = 14.sp,
            color = Color(0xFFAAAAAA)
        )

        Spacer(Modifier.height(40.dp))

        // Card คำแนะนำ
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E))
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("วิธีใช้", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "1. กดปุ่มเปิดสิทธิ์ Floating Window\n" +
                    "2. กดเริ่มจับภาพหน้าจอ แล้วอนุญาต\n" +
                    "3. เปิดเกม Block Blast!\n" +
                    "4. คำแนะนำจะโผล่เป็นหน้าต่างลอย\n" +
                    "5. ลากย้ายหน้าต่างได้ตามสะดวก",
                    color = Color(0xFFCCCCCC),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ปุ่มสิทธิ์ Overlay
        OutlinedButton(
            onClick = onRequestOverlay,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00D4FF))
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("เปิดสิทธิ์ Floating Window")
        }

        Spacer(Modifier.height(16.dp))

        // ปุ่มหลัก
        Button(
            onClick = if (isCapturing) onStop else onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCapturing) Color(0xFFE94560) else Color(0xFF00D4FF)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                if (isCapturing) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (isCapturing) "หยุดจับภาพ" else "เริ่มจับภาพหน้าจอ",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "หมายเหตุ: การตรวจจับกระดานอัตโนมัติยังเป็นเวอร์ชันพื้นฐาน\n" +
                    "สามารถพัฒนา BoardDetector.kt เพิ่มเติมได้",
            color = Color(0xFF888888),
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun BlockBlastHelperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF00D4FF),
            background = Color(0xFF1A1A2E),
            surface = Color(0xFF16213E)
        ),
        content = content
    )
}
