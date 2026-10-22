package com.blockblast.helper.solver

import com.blockblast.helper.data.*

/**
 * Solver หลักสำหรับ Block Blast
 * เน้น: รอดนาน + คอมโบ + รักษาพื้นที่ 3x3
 */
class BlockSolver {

    /**
     * หาลำดับการวางที่ดีที่สุดสำหรับชิ้น 3 ชิ้น
     */
    fun findBestSequence(state: GameState): Suggestion {
        val pieces = state.pieces
        if (pieces.isEmpty()) {
            return Suggestion(emptyList(), 0, 0, "ไม่มีชิ้นบล็อก")
        }

        val permutations = permuteIndices(pieces.size)
        var best: Suggestion? = null

        for (order in permutations) {
            val result = evaluateOrder(state.board, pieces, order)
            if (result != null) {
                if (best == null || isBetter(result, best)) {
                    best = result
                }
            }
        }

        return best ?: Suggestion(
            emptyList(), 0, 0,
            "ไม่พบตำแหน่งที่วางได้ทั้งหมด — เสี่ยงเกมจบ"
        )
    }

    private fun evaluateOrder(
        startBoard: Board,
        pieces: List<Shape>,
        order: List<Int>
    ): Suggestion? {
        var board = startBoard.copy()
        val placements = mutableListOf<Placement>()
        var totalLines = 0
        var totalScore = 0

        for (idx in order) {
            val shape = pieces[idx]
            val candidates = findAllPlacements(board, shape, idx)

            if (candidates.isEmpty()) return null

            // เลือกตำแหน่งที่ดีที่สุดสำหรับชิ้นนี้
            val bestPlace = candidates.maxWithOrNull(
                compareByDescending<Placement> { it.linesCleared }
                    .thenByDescending { it.keeps3x3 }
                    .thenByDescending { it.remainingEmpty }
                    .thenByDescending { it.scoreEstimate }
            ) ?: return null

            // วางจริง
            val placed = board.place(shape, bestPlace.row, bestPlace.col) ?: return null
            val (clearedBoard, lines) = placed.clearLines()
            board = clearedBoard
            totalLines += lines
            totalScore += bestPlace.scoreEstimate + lines * 80

            placements.add(bestPlace.copy(linesCleared = lines))
        }

        val reason = buildReason(placements, totalLines, board)
        return Suggestion(placements, totalLines, totalScore, reason)
    }

    private fun findAllPlacements(board: Board, shape: Shape, shapeIndex: Int): List<Placement> {
        val results = mutableListOf<Placement>()
        for (r in 0 until 8) {
            for (c in 0 until 8) {
                val placed = board.place(shape, r, c) ?: continue
                val (afterClear, lines) = placed.clearLines()
                val keeps3x3 = afterClear.hasOpen3x3()
                val empty = afterClear.emptyCount()
                val score = lines * 100 + if (keeps3x3) 50 else 0 + empty

                results.add(
                    Placement(
                        shapeIndex = shapeIndex,
                        row = r,
                        col = c,
                        linesCleared = lines,
                        scoreEstimate = score,
                        keeps3x3 = keeps3x3,
                        remainingEmpty = empty
                    )
                )
            }
        }
        return results
    }

    private fun isBetter(a: Suggestion, b: Suggestion): Boolean {
        // ลำดับความสำคัญ: วางได้ครบ > เคลียร์เส้นมาก > รักษา 3x3 > คะแนน
        if (a.placements.size != b.placements.size) {
            return a.placements.size > b.placements.size
        }
        if (a.totalLines != b.totalLines) return a.totalLines > b.totalLines
        val aKeep = a.placements.count { it.keeps3x3 }
        val bKeep = b.placements.count { it.keeps3x3 }
        if (aKeep != bKeep) return aKeep > bKeep
        return a.totalScore > b.totalScore
    }

    private fun buildReason(
        placements: List<Placement>,
        totalLines: Int,
        finalBoard: Board
    ): String {
        val sb = StringBuilder()
        if (totalLines > 0) {
            sb.append("คาดว่าจะเคลียร์ $totalLines เส้น → คอมโบดี\n")
        }
        if (finalBoard.hasOpen3x3()) {
            sb.append("ยังเหลือพื้นที่ 3×3 ว่าง (ปลอดภัยจากชิ้นใหญ่)\n")
        } else {
            sb.append("⚠️ ไม่เหลือ 3×3 ว่าง — ระวังชิ้น 3×3 ชุดหน้า\n")
        }
        sb.append("ช่องว่างเหลือประมาณ ${finalBoard.emptyCount()} ช่อง")
        return sb.toString()
    }

    private fun permuteIndices(n: Int): List<List<Int>> {
        if (n <= 1) return listOf((0 until n).toList())
        val result = mutableListOf<List<Int>>()
        fun backtrack(path: MutableList<Int>, used: BooleanArray) {
            if (path.size == n) {
                result.add(path.toList())
                return
            }
            for (i in 0 until n) {
                if (used[i]) continue
                used[i] = true
                path.add(i)
                backtrack(path, used)
                path.removeAt(path.lastIndex)
                used[i] = false
            }
        }
        backtrack(mutableListOf(), BooleanArray(n))
        return result
    }
}
