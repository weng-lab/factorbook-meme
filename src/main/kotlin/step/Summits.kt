package step

import mu.KotlinLogging
import java.nio.file.*
import java.util.zip.GZIPInputStream

private val log = KotlinLogging.logger {}

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

/**
 * Resizes peaks to a fixed width around their midpoint, excluding any peaks which extend off the chromosome,
 * and sorts results by Q-value then P-value then signal. If an offset is passed, shifts peaks by a set
 * number of basepairs.
 *
 * @param peaks path to peaks to resize.
 * @param chromSizes Map of chromosomes to sizes for this assembly.
 * @param newSize fixed width to which peaks should be resized.
 * @param output path to write resized output peaks.
 * @param offset number of base pairs to shift chrom start and end by (Optional)
 */
fun summits(peaks: Path, chromSizes: Map<String, Int>, newSize: Int, output: Path, offset: Int? = null,
            chrFilter: Set<String>? = null) {
    log.info {
        """
        Creating summits for
        peaks: $peaks
        chromSizes $chromSizes
        newSize: $newSize
        offset: $offset
        output: $output
        chromFilter: $chrFilter
        """.trimIndent()
    }
    val rawInputStream = Files.newInputStream(peaks)
    val inputStream = if (peaks.endsWith(".gz")) GZIPInputStream(rawInputStream) else rawInputStream
    val clippedRows = mutableListOf<PeaksRow>()
    inputStream.reader().forEachLine { line ->
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
        if (chrFilter != null && chrFilter.contains(rawRow.chrom)) return@forEachLine

        val chromSize = chromSizes[rawRow.chrom] ?: return@forEachLine

        val chromEnd = if (offset != null) {
            val newEnd = rawRow.chromEnd + offset
            if (newEnd < chromSize) newEnd else chromSize
        } else rawRow.chromEnd

        val chromStart = if (offset != null) {
            val newStart = rawRow.chromStart + offset
            if (newStart < chromEnd) newStart else chromEnd - 1
        } else rawRow.chromStart

        val midpoint = (chromStart + chromEnd) / 2
        if (midpoint < newSize || midpoint + newSize > chromSize) return@forEachLine

        val newStart = midpoint - newSize
        val newEnd = midpoint + newSize
        val clippedRow = rawRow.copy(chromStart = newStart, chromEnd = newEnd)
        clippedRows.add(clippedRow)
    }

    clippedRows.sortWith(compareBy({ it.qValue }, { it.pValue }, { it.signalValue }))

    Files.createDirectories(output.parent)
    Files.newBufferedWriter(output).use { writer ->
        for(row in clippedRows) {
            val rowStr = with(row) {
                "$chrom\t$chromStart\t$chromEnd\t$name\t$score\t$strand\t$signalValue\t$pValue\t$qValue\t$peak\n"
            }
            writer.write(rowStr)
        }
    }
}

/**
 * Construct map of chromosome names to sizes from chrom sizes file
 *
 * @param from path to chrom sizes file
 */
fun parseChromSizes(from: Path): Map<String, Int> {
    val lines = Files.readAllLines(from)
    return lines.map { line ->
        val parts = line.split("\t")
        parts[0] to parts[1].toInt()
    }.toMap()
}



