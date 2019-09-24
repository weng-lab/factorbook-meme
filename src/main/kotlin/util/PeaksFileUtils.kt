package util

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPInputStream

data class PeaksRow(
        val chrom: String,
        val chromStart: Int,
        val chromEnd: Int,
        val name: String,
        val score: Int,
        val strand: Char,
        val signalValue: Double,
        val pValue: Double,
        val qValue: Double,
        val peak: Int
)

fun readPeaksFile(peaks: Path, lineRange: IntRange? = null, handlePeaksRow: (PeaksRow) -> Unit) {
    val rawInputStream = Files.newInputStream(peaks)
    val inputStream = if (peaks.toString().endsWith(".gz")) GZIPInputStream(rawInputStream) else rawInputStream
    inputStream.reader().useLines { lines ->
        lines.forEachIndexed { index, line ->
            if (lineRange != null && !lineRange.contains(index)) return@forEachIndexed
            val lineParts = line.trim().split("\t")
            val rawRow = PeaksRow(
                    chrom = lineParts[0],
                    chromStart = lineParts[1].toInt(),
                    chromEnd = lineParts[2].toInt(),
                    name = lineParts[3],
                    score = lineParts[4].toInt(),
                    strand = lineParts[5][0],
                    signalValue = lineParts[6].toDouble(),
                    pValue = lineParts[7].toDouble(),
                    qValue = lineParts[8].toDouble(),
                    peak = lineParts[9].toInt()
            )
            handlePeaksRow(rawRow)
        }
    }
}

fun writePeaksFile(out: Path, rows: List<PeaksRow>) {
    Files.createDirectories(out.parent)
    Files.newBufferedWriter(out).use { writer ->
        for(row in rows) {
            val rowStr = with(row) {
                "$chrom\t$chromStart\t$chromEnd\t$name\t$score\t$strand\t$signalValue\t$pValue\t$qValue\t$peak\n"
            }
            writer.write(rowStr)
        }
    }
}