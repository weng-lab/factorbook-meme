package step

import mu.KotlinLogging
import util.CmdRunner
import java.nio.file.Files
import java.nio.file.Path

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
                        seqsOut: Path, seqsCenteredOut: Path, seqsFlanksOut: Path) {
    trimPeaks(summits, lineRange, trimmedPeaksOut)
    peaksToFasta(trimmedPeaksOut, twoBit, seqsOut)
    fastaCenter(seqsOut, len, seqsFlanksOut, seqsCenteredOut)
}

/**
 * Produces a peaks file with only the given range of rows and only each row containing only the first four fields.
 *
 * @param peaks path to the peak file.
 * @param lineRange range of lines to select from the peak file.
 * @param output output path to write the trimmed peaks.
 */
fun trimPeaks(peaks: Path, lineRange: IntRange, output: Path) {
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
                if (index < lineRange.start) return@forEachIndexed
                if (index > lineRange.endInclusive) return@useLines
                val lineParts = line.trim().split("\t")
                writer.write(lineParts.subList(0,4).joinToString("\t", postfix = "\n"))
            }
        }
    }
}

/**
 * Produces a FASTA for a subset of lines in a peak file.
 *
 * @param trimmedPeaks path to the trimmed peaks file.
 * @param twoBit path to the two bit sequence file for this genome.
 * @param output output path to write the FASTA.
 */
fun CmdRunner.peaksToFasta(trimmedPeaks: Path, twoBit: Path, output: Path) {
    Files.createDirectories(output.parent)
    this.run("twoBitToFa $twoBit $output -bed=$trimmedPeaks")
}

/**
 * Calls fasta-center on a FASTA File.
 *
 * @param fastaIn input FASTA file.
 * @param len length of sequences to output.
 * @param flank path to write flanking sequences file.
 * @param output path to write output FASTA file.
 */
fun CmdRunner.fastaCenter(fastaIn: Path, len: Int, flank: Path, output: Path) {
    Files.createDirectories(output.parent)
    this.run("fasta-center -len $len -flank $flank < $fastaIn > $output")
}
