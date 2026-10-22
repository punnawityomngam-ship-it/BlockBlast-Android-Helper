package com.blockblast.helper.vision

import android.graphics.Bitmap
import android.graphics.Color
import com.blockblast.helper.data.Board
import com.blockblast.helper.data.Shape

/**
 * ตัวตรวจจับกระดานจากภาพหน้าจอ
 *
 * หมายเหตุ: เวอร์ชันนี้เป็น heuristic พื้นฐาน
 * ในโปรดักชันจริงควรใช้ ML (TFLite / ML Kit) หรือ OpenCV ที่เทรนกับ UI ของเกม
 */
class BoardDetector {

    /**
     * วิเคราะห์ภาพหน้าจอ → คืน Board + ชิ้นบล็อกโดยประมาณ
     * ตอนนี้คืนค่า mock เพื่อให้โครงสร้างทำงานได้
     * คุณสามารถแทนที่ด้วย logic จริงได้ทีหลัง
     */
    fun detect(bitmap: Bitmap): DetectionResult {
        // TODO: ใส่ computer vision จริงตรงนี้
        // 1. หาตำแหน่งกริด 8x8 (มักอยู่ตรงกลางหน้าจอ)
        // 2. แบ่งเป็น 64 ช่อง แล้วดูสี/ความสว่าง
        // 3. หาชิ้นบล็อกด้านล่าง

        // ตอนนี้คืนกระดานว่าง + ชิ้นตัวอย่าง เพื่อทดสอบ flow
        val board = Board()
        val pieces = listOf(
            Shape.SQUARE_2,
            Shape.L_SMALL,
            Shape.LINE_3H
        )
        return DetectionResult(board, pieces, confidence = 0.0f)
    }

    /**
     * เวอร์ชันง่าย: ผู้ใช้แตะเพื่อบอกตำแหน่งกริดเอง (manual calibration)
     */
    fun detectWithRegion(
        bitmap: Bitmap,
        gridLeft: Int,
        gridTop: Int,
        gridSize: Int
    ): Board {
        val cellSize = gridSize / 8
        val board = Board()

        for (r in 0 until 8) {
            for (c in 0 until 8) {
                val x = gridLeft + c * cellSize + cellSize / 2
                val y = gridTop + r * cellSize + cellSize / 2
                if (x < bitmap.width && y < bitmap.height) {
                    val pixel = bitmap.getPixel(x, y)
                    // ถ้าสีไม่ใช่พื้นหลังมืด → มีบล็อก
                    val brightness = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                    board.cells[r][c] = brightness > 40
                }
            }
        }
        return board
    }
}

data class DetectionResult(
    val board: Board,
    val pieces: List<Shape>,
    val confidence: Float
)
