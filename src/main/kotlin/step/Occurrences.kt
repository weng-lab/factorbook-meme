package step

import java.nio.file.*

/**
 * Translate FIMO tsv's into occurrence tsv's that use absolute positioning
 */
fun occurrencesTsv(fimoTsv: Path, peaksBed: Path, out: Path) {
    val peaks = parsePeaksBed(peaksBed)
    Files.createDirectories(out.parent)
    Files.newBufferedWriter(out, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use { writer ->
        writer.write("#motif_id\tchromosome\tstart\tend\tstrand\tq_value\n")
        Files.newBufferedReader(fimoTsv).forEachLine { line ->
            if (line.isBlank() || line.startsWith("motif_id") || line.startsWith("#")) return@forEachLine
            val lineParts = line.split("\\s".toRegex())
            val motifId = lineParts[0]
            val peakId = lineParts[2]
            val startWithinPeak = lineParts[3].toInt()
            val endWithinPeak = lineParts[4].toInt()
            val strand = lineParts[5]
            val qValue = lineParts[8].toDouble()
            val peak = peaks.getValue(peakId)
            val absoluteStart = peak.start + startWithinPeak - 1
            val absoluteEnd = peak.start + endWithinPeak
            writer.write("$motifId\t${peak.chr}\t$absoluteStart\t$absoluteEnd\t$strand\t$qValue\n")
        }
    }
}

private data class Peak(
        val peakId: String,
        val chr: String,
        val start: Int
)

private fun parsePeaksBed(peaksBed: Path): Map<String, Peak> {
    val peaks = mutableMapOf<String, Peak>()
    Files.newBufferedReader(peaksBed).forEachLine { line ->
        if (line.isBlank()) return@forEachLine
        val lineParts = line.split("\\s".toRegex())
        val peak = Peak(lineParts[3], lineParts[0], lineParts[1].toInt())
        peaks[peak.peakId] = peak
    }
    return peaks
}