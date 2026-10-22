package com.blockblast.helper.data

/**
 * กระดาน 8x8
 * true = มีบล็อก, false = ว่าง
 */
data class Board(
    val cells: Array<BooleanArray> = Array(8) { BooleanArray(8) }
) {
    fun copy(): Board {
        val newCells = Array(8) { r -> cells[r].copyOf() }
        return Board(newCells)
    }

    fun isEmpty(row: Int, col: Int): Boolean =
        row in 0..7 && col in 0..7 && !cells[row][col]

    fun place(shape: Shape, row: Int, col: Int): Board? {
        val newBoard = copy()
        for (p in shape.points) {
            val r = row + p.first
            val c = col + p.second
            if (r !in 0..7 || c !in 0..7 || newBoard.cells[r][c]) return null
            newBoard.cells[r][c] = true
        }
        return newBoard
    }

    /** เคลียร์แถวและคอลัมน์ที่เต็ม แล้วคืนจำนวนเส้นที่เคลียร์ */
    fun clearLines(): Pair<Board, Int> {
        val newBoard = copy()
        val fullRows = (0..7).filter { r -> newBoard.cells[r].all { it } }
        val fullCols = (0..7).filter { c -> (0..7).all { r -> newBoard.cells[r][c] } }

        fullRows.forEach { r ->
            for (c in 0..7) newBoard.cells[r][c] = false
        }
        fullCols.forEach { c ->
            for (r in 0..7) newBoard.cells[r][c] = false
        }
        return newBoard to (fullRows.size + fullCols.size)
    }

    fun emptyCount(): Int = cells.sumOf { row -> row.count { !it } }

    /** หาพื้นที่ 3x3 ว่างที่ใหญ่ที่สุด (สำหรับป้องกัน 3x3 ชิ้น) */
    fun hasOpen3x3(): Boolean {
        for (r in 0..5) {
            for (c in 0..5) {
                var ok = true
                for (dr in 0..2) {
                    for (dc in 0..2) {
                        if (cells[r + dr][c + dc]) {
                            ok = false
                            break
                        }
                    }
                    if (!ok) break
                }
                if (ok) return true
            }
        }
        return false
    }
}

/**
 * รูปร่างบล็อก (relative points จากจุดบนซ้าย)
 */
data class Shape(
    val points: List<Pair<Int, Int>>,
    val name: String = ""
) {
    val width: Int get() = points.maxOf { it.second } + 1
    val height: Int get() = points.maxOf { it.first } + 1

    companion object {
        // ตัวอย่างชิ้นที่ใช้บ่อย
        val SINGLE = Shape(listOf(0 to 0), "1x1")
        val DOMINO_H = Shape(listOf(0 to 0, 0 to 1), "2H")
        val DOMINO_V = Shape(listOf(0 to 0, 1 to 0), "2V")
        val SQUARE_2 = Shape(listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1), "2x2")
        val LINE_3H = Shape(listOf(0 to 0, 0 to 1, 0 to 2), "3H")
        val LINE_3V = Shape(listOf(0 to 0, 1 to 0, 2 to 0), "3V")
        val LINE_4H = Shape(listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3), "4H")
        val LINE_5H = Shape(listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4), "5H")
        val SQUARE_3 = Shape(
            listOf(
                0 to 0, 0 to 1, 0 to 2,
                1 to 0, 1 to 1, 1 to 2,
                2 to 0, 2 to 1, 2 to 2
            ), "3x3"
        )
        val L_SMALL = Shape(listOf(0 to 0, 1 to 0, 1 to 1), "L")
        val T = Shape(listOf(0 to 0, 0 to 1, 0 to 2, 1 to 1), "T")
    }
}

data class Placement(
    val shapeIndex: Int,
    val row: Int,
    val col: Int,
    val linesCleared: Int,
    val scoreEstimate: Int,
    val keeps3x3: Boolean,
    val remainingEmpty: Int
)

data class Suggestion(
    val placements: List<Placement>,   // ลำดับการวางทั้ง 3 ชิ้น
    val totalLines: Int,
    val totalScore: Int,
    val reason: String
)

data class GameState(
    val board: Board,
    val pieces: List<Shape>
)
