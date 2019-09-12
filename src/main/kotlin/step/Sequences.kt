package step

import mu.KotlinLogging
import util.CmdRunner
import java.nio.file.*

private val log = KotlinLogging.logger {}

/**
 * Produces FASTA sequence files the given range of resized and shifted input peaks.
 *
 * @param summits path to summits file containing resized and shifted peaks
 * @param lineRange range of lines to select from the peak file.
 * @param len length of sequences to output
 * @param trimmedPeaksOut path of trimmed peaks file to output
 * @param seqsOut path of fasta file to output
 * @param seqsCenteredOut path of centered fasta file to output
 */
fun CmdRunner.sequences(summits: Path, twoBit: Path, lineRange: IntRange, len: Int, trimmedPeaksOut: Path,
                        seqsOut: Path, seqsCenteredOut: Path, seqsFlanksOut: Path? = null,
                        chrFilter: Set<String>? = null) {
    trimPeaks(summits, trimmedPeaksOut, lineRange)
    peaksToFasta(trimmedPeaksOut, twoBit, seqsOut)
    fastaCenter(seqsOut, len, seqsCenteredOut, seqsFlanksOut)
}

/**
 * Produces a peaks file with only the given range of rows and only each row containing only the first four fields.
 *
 * @param peaks path to the peak file.
 * @param output output path to write the trimmed peaks.
 * @param lineRange range of lines to select from the peak file. (Optional)
 */
fun trimPeaks(peaks: Path, output: Path, lineRange: IntRange? = null, chrFilter: Set<String>? = null) {
    log.info {
        """
        Trimming peaks for
        peaks: $peaks
        lineRange: $lineRange
        output: $output
        """.trimIndent()
    }
    Files.createDirectories(output.parent)
    Files.newBufferedWriter(output).use { writer ->
        Files.newInputStream(peaks).reader().useLines { lines ->
            lines.forEachIndexed { index, line ->
                if (lineRange != null && index < lineRange.first) return@forEachIndexed
                if (lineRange != null && index > lineRange.last) return@useLines
                val lineParts = line.trim().split("\t")
                if (chrFilter != null && chrFilter.contains(lineParts[0])) return@forEachIndexed
                writer.write(lineParts.subList(0,4).joinToString("\t", postfix = "\n"))
            }
        }
    }
}

/**
 * Produces a FASTA for a subset of lines in a peak file.
 *
 * @param peaks path to the trimmed peaks file.
 * @param twoBit path to the two bit sequence file for this genome.
 * @param output output path to write the FASTA.
 */
fun CmdRunner.peaksToFasta(peaks: Path, twoBit: Path, output: Path) {
    Files.createDirectories(output.parent)
    this.run("twoBitToFa $twoBit $output -bed=$peaks")
}

/**
 * Calls fasta-center on a FASTA File.
 *
 * @param fastaIn input FASTA file.
 * @param len length of sequences to output.
 * @param output path to write output FASTA file.
 * @param flank path to write flanking sequences file (optional)
 */
fun CmdRunner.fastaCenter(fastaIn: Path, len: Int, output: Path, flank: Path? = null) {
    Files.createDirectories(output.parent)
    val flankArgs = if (flank != null) "-flank $flank " else ""
    this.run("fasta-center -len $len $flankArgs< $fastaIn > $output")
}
