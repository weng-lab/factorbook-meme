import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.*
import mu.KotlinLogging
import step.*
import util.*
import java.nio.file.*

private val log = KotlinLogging.logger {}

fun main(args: Array<String>) = Cli().main(args)

class Cli : CliktCommand() {

    private val peaks by option("--peaks", help = "path to peaks in narrowPeak format")
        .path(exists = true).required()
    private val twoBit by option("--twobit", help = "path to two-bit file for this assembly")
        .path(exists = true).required()
    private val chromInfo by option("--chrom-info", help = "path to chromosome lengths for this assembly")
        .path(exists = true).required()
    private val offset by option("--offset", help = "offset, in bp, to shift peaks")
        .int().default(0)
    private val outputDir by option("--output-dir", help = "path to write output")
        .path().required()

    override fun run() {
        val cmdRunner = DefaultCmdRunner()
        cmdRunner.runTask(peaks, twoBit, chromInfo, offset, outputDir)
    }
}

/**
 * Runs pre-processing and meme for raw input files
 *
 * @param peaks path to raw narrowPeaks file
 * @param chromInfo path to chromInfo file
 * @param offset
 * @param outputDir
 */
fun CmdRunner.runTask(peaks: Path, twoBit: Path, chromInfo: Path, offset: Int, outputDir: Path) {
    log.info {
        """
        Running Meme task for
        peaks: $peaks
        twoBit: $twoBit
        chromInfo: $chromInfo
        offset: $offset
        outputDir: $outputDir
        """.trimIndent()
    }
    val outPrefix = peaks.fileName.toString().split(".").first()

    // Create fasta file containing sequences for original input peaks file
    val trimmedPeaks = outputDir.resolve("$outPrefix.narrowPeak.trimmed")
    trimPeaks(peaks, trimmedPeaks)
    val originalPeaksFastaFile = outputDir.resolve("$outPrefix.seqs")
    peaksToFasta(trimmedPeaks, twoBit, originalPeaksFastaFile)

    // Create summits file
    val chromSizes = parseChromSizes(chromInfo)
    val summitsFile = outputDir.resolve("$outPrefix.summits.window150.narrowPeak")
    summits(peaks, chromSizes, 150, summitsFile, offset)

    // Run MEME on top 500 peaks
    val top500Prefix = "$outPrefix.top500"
    val top500PeaksFile = outputDir.resolve("$top500Prefix.narrowPeak.trimmed")
    val top500SeqsFile = outputDir.resolve("$top500Prefix.seqs")
    val top500CenterSeqsFile = outputDir.resolve("$top500Prefix.seqs.center")
    sequences(summitsFile, twoBit, 0 until 500, 100, top500PeaksFile, top500SeqsFile, top500CenterSeqsFile)

    val memeOutDir = outputDir.resolve("$outPrefix.top500.center.meme")
    meme(top500CenterSeqsFile, memeOutDir)

    // Run FIMO against original peaks sequences
    val memeTxtFile = memeOutDir.resolve("meme.txt")
    val originalPeaksFimoDir = outputDir.resolve("$outPrefix.fimo")
    fimo(memeTxtFile, originalPeaksFastaFile, originalPeaksFimoDir)

    // Convert FIMO Occurrences to custom Occurrences TSV with absolute positioned ranges
    val originalPeaksFimoTsv = originalPeaksFimoDir.resolve("fimo.tsv")
    val occurrencesTsv = outputDir.resolve("$outPrefix.occurrences.tsv")
    occurrencesTsv(originalPeaksFimoTsv, peaks, occurrencesTsv)

    // Run FIMO against peaks 501-1000 center and flanks
    val next500Prefix = "$outPrefix.top501-1000"
    val next500PeaksFile = outputDir.resolve("$next500Prefix.narrowPeak.trimmed")
    val next500SeqsFile = outputDir.resolve("$next500Prefix.seqs")
    val next500CenterSeqsFile = outputDir.resolve("$next500Prefix.seqs.center")
    val next500FlankSeqsFile = outputDir.resolve("$next500Prefix.seqs.flank")
    sequences(summitsFile, twoBit, 501 until 1000, 100, next500PeaksFile, next500SeqsFile,
            next500CenterSeqsFile, next500FlankSeqsFile)

    val next500CenterFimoDir = outputDir.resolve("$next500Prefix.center.fimo")
    fimo(memeTxtFile, next500CenterSeqsFile, next500CenterFimoDir)

    val next500FlankFimoDir = outputDir.resolve("$next500Prefix.flank.fimo")
    fimo(memeTxtFile, next500FlankSeqsFile, next500FlankFimoDir)

    // Run FIMO against 100x random sequences from reference genome (with matching length and gc content)
    val randomSeqFile = outputDir.resolve("$next500Prefix.shuffled.seqs")
    val randomFimoDir = outputDir.resolve("$next500Prefix.shuffled.fimo")
    randomSequences(twoBit, top500CenterSeqsFile, randomSeqFile, 100, chromSizes, 0.05)
    fimo(memeTxtFile, randomSeqFile, randomFimoDir)

    // Run Motif Quality step
    val memeXmlFile = memeOutDir.resolve("meme.xml")
    val outJsonFile = outputDir.resolve("$outPrefix.motifs.json")
    motifQuality(memeXmlFile, next500CenterFimoDir, randomFimoDir, next500FlankFimoDir, outJsonFile)
}