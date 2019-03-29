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
    val chromSizes = parseChromSizes(chromInfo)
    val summitsFile = outputDir.resolve("$outPrefix.summits.window150.narrowPeak")
    summits(peaks, chromSizes, 150, summitsFile, offset)

    val ranges = mapOf("top500" to (0..500), "top501-1000" to (500..1000))
    for ((rangePrefix, range) in ranges) {
        val trimmedPeaksFile = outputDir.resolve("$outPrefix.$rangePrefix.narrowPeak.trimmed")
        val seqsFile = outputDir.resolve("$outPrefix.$rangePrefix.seqs")
        val centerSeqsFile = outputDir.resolve("$outPrefix.$rangePrefix.seqs.center")
        val flankSeqsFile = outputDir.resolve("$outPrefix.$rangePrefix.seqs.flank")
        sequences(summitsFile, twoBit, range, 100, trimmedPeaksFile, seqsFile, centerSeqsFile, flankSeqsFile)

        val memeFile = outputDir.resolve("$outPrefix.$rangePrefix.center.meme")
        meme(centerSeqsFile, memeFile)
    }
}