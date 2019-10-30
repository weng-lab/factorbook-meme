package step

import util.readFimoTsv
import java.nio.file.*

/**
 * Translate FIMO tsv's into occurrence tsv's that use absolute positioning
 */
fun occurrencesTsv(fimoTsv: Path, peaksBed: Path, out: Path) {
    val peaks = parsePeaksBed(peaksBed)
    Files.createDirectories(out.parent)
    Files.newBufferedWriter(out, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use { writer ->
        writer.write("#motif_id\tchromosome\tstart\tend\tstrand\tq_value\n")
        readFimoTsv(fimoTsv) { row ->
            val peak = peaks.getValue(row.peakId)
            val absoluteStart = peak.start + row.relativeStart - 1
            val absoluteEnd = peak.start + row.relativeEnd
            writer.write("${row.motifId}\t${peak.chr}\t$absoluteStart\t$absoluteEnd\t${row.strand}\t${row.qValue}\n")
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