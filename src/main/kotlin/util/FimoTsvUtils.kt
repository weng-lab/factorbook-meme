package util

import java.nio.file.*

data class FimoTsvRow(
        val motifId: String,
        val peakId: String,
        val relativeStart: Int,
        val relativeEnd: Int,
        val strand: String,
        val qValue: Double
)

fun readFimoTsv(fimoTsv: Path, handlePeaksRow: (FimoTsvRow) -> Unit) {
    Files.newBufferedReader(fimoTsv).forEachLine { line ->
        if (line.isBlank() || line.startsWith("motif_id") || line.startsWith("#")) return@forEachLine
        val lineParts = line.split("\\s".toRegex())
        val row = FimoTsvRow(motifId = lineParts[0], peakId = lineParts[2], relativeStart = lineParts[3].toInt(),
                relativeEnd = lineParts[4].toInt(), strand = lineParts[5], qValue = lineParts[8].toDouble())
        handlePeaksRow(row)
    }
}
