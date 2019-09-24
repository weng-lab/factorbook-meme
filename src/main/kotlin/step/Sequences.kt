package step

import mu.KotlinLogging
import org.biojava.nbio.genome.parsers.twobit.TwoBitParser
import util.*
import java.nio.file.*

private val log = KotlinLogging.logger {}

/**
 * Produces a FASTA for a subset of lines in a peak file.
 *
 * @param peaks path to the trimmed peaks file.
 * @param twoBit path to the two bit sequence file for this genome.
 * @param output output path to write the FASTA.
 * @param methylBed optional methylation states bed file used to add 'M' and 'W'
 *      methylation indicator replacements in sequences.
 * @param methylPercentThreshold
 */
fun peaksToFasta(peaks: Path, twoBit: Path, output: Path, methylBed: Path? = null, methylPercentThreshold: Int? = null,
                 lineRange: IntRange? = null) {
    // Keep one parser per chromosome, because the "setCurrentSequence" method takes time and caches header data.
    val parsers = mutableMapOf<String, TwoBitParser>()
    val methylData = if (methylBed != null) parseMethylBed(methylBed, methylPercentThreshold) else null

    Files.createDirectories(output.parent)
    Files.newBufferedWriter(output).use { writer ->
        readPeaksFile(peaks, lineRange) { peaksRow ->
            val parser = parsers.getOrPut(peaksRow.chrom) {
                val p = TwoBitParser(twoBit.toFile())
                p.setCurrentSequence(peaksRow.chrom)
                p
            }
            var segment = parser.loadFragment(peaksRow.chromStart.toLong(), peaksRow.chromEnd - peaksRow.chromStart)
            if (methylData != null) {
                segment = methylData.replaceBases(segment, peaksRow.chrom, peaksRow.chromStart .. peaksRow.chromEnd)
            }

            segment = segment.chunked(50).joinToString("\n")
            writer.write(">${peaksRow.name}\n$segment\n")
        }
    }
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
