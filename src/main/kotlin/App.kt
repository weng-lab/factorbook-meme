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
    private val chrFilter by option("--chrom-filter",
            help = "chromosomes to filter out before running MEME.").multiple()
    private val methylBed by option("--methyl-bed", help = "path to optional methylation state @CpG bed file")
            .path(exists = true)
    private val methylPercentThreshold by option("--methyl-percent-threshold",
            help = "the percentage over which we will use a methylation site from the methylation bed file.")
            .int()

    override fun run() {
        val cmdRunner = DefaultCmdRunner()
        cmdRunner.runTask(peaks, twoBit, chromInfo, offset, outputDir, chrFilter.toSet(), methylBed, methylPercentThreshold)
    }
}

/**
 * Runs pre-processing and meme for raw input files
 *
 * @param peaks path to raw narrowPeaks file
 * @param chromInfo path to chromInfo file
 * @param offset amount to shift peaks when creating summits
 * @param outputDir directory to put output files
 * @param chrFilter set of chromosomes to filter out before running
 * @param methylBed methylated state @CpG file used for runs that create motifs with methyl base pairs
 * @param methylPercentThreshold the percentage over which we will use a methylation site
 */
fun CmdRunner.runTask(peaks: Path, twoBit: Path, chromInfo: Path, offset: Int, outputDir: Path,
                      chrFilter: Set<String>? = null, methylBed: Path? = null, methylPercentThreshold: Int? = null) {
    log.info {
        """
        Running Meme task for
        peaks: $peaks
        twoBit: $twoBit
        chromInfo: $chromInfo
        offset: $offset
        outputDir: $outputDir
        chromFilter: $chrFilter
        methylBed: $methylBed
        methylPercentThreshold: $methylPercentThreshold
        """.trimIndent()
    }
    val outPrefix = peaks.fileName.toString().split(".").first()
    val chromSizes = parseChromSizes(chromInfo)

    // Rewrite peaks names, apply chrom filter and filter peaks without methylated states (if methyl bed is given)
    // Name rewrite is necessary because given peaks input may not include them
    val cleanedPeaks = outputDir.resolve("$outPrefix$CLEANED_BED_SUFFIX")
    cleanPeaks(peaks, chrFilter, methylBed, methylPercentThreshold, cleanedPeaks)

    runMemeSteps(outPrefix, cleanedPeaks, twoBit, chromSizes, offset, outputDir, chrFilter,
            methylBed, methylPercentThreshold)

    val summitsFile = outputDir.resolve("$outPrefix$SUMMITS_FILE_SUFFIX")
    val memeOutDir = outputDir.resolve("$outPrefix$MEME_DIR_SUFFIX")
    val top500CenterSeqsFile = outputDir.resolve("$outPrefix$TOP500_SEQS_CENTER_SUFFIX")
    runQualitySteps(outPrefix, summitsFile, memeOutDir, top500CenterSeqsFile, twoBit, outputDir, chromSizes,
            methylBed, methylPercentThreshold)

    runOccurrencesSteps(outPrefix, cleanedPeaks, twoBit, outputDir, methylBed, methylPercentThreshold)
}

/**
 * Run Meme pre-processing and Meme steps
 */
fun CmdRunner.runMemeSteps(outPrefix: String, cleanedPeaks: Path, twoBit: Path, chromSizes: Map<String, Int>,
                           offset: Int, outputDir: Path, chrFilter: Set<String>? = null,
                           methylBed: Path? = null, methylPercentThreshold: Int? = null) {
    // Create summits file
    val summitsFile = outputDir.resolve("$outPrefix$SUMMITS_FILE_SUFFIX")
    summits(cleanedPeaks, chromSizes, 150, summitsFile, offset, chrFilter)

    // Run MEME on top 500 peaks
    val top500SeqsFile = outputDir.resolve("$outPrefix$TOP500_SEQS_SUFFIX")
    val top500CenterSeqsFile = outputDir.resolve("$outPrefix$TOP500_SEQS_CENTER_SUFFIX")
    peaksToFasta(summitsFile, twoBit, top500SeqsFile, methylBed, methylPercentThreshold,
            0 until 500)
    fastaCenter(top500SeqsFile, 100, top500CenterSeqsFile)

    val memeOutDir = outputDir.resolve("$outPrefix$MEME_DIR_SUFFIX")
    val useMotifAlphabet = methylBed != null
    meme(top500CenterSeqsFile, memeOutDir, useMotifAlphabet)
}

/**
 * Run post-Meme quality related steps
 */
fun CmdRunner.runQualitySteps(outPrefix: String, summitsFile: Path, memeDir: Path, top500CenterSeqsFile: Path,
                              twoBit: Path, outputDir: Path, chromSizes: Map<String, Int>,
                              methylBed: Path? = null, methylPercentThreshold: Int? = null) {
    // Run FIMO against peaks 501-1000 center and flanks
    val next500SeqsFile = outputDir.resolve("$outPrefix$NEXT500_SEQS_SUFFIX")
    val next500CenterSeqsFile = outputDir.resolve("$outPrefix$NEXT500_SEQS_CENTER_SUFFIX")
    val next500FlankSeqsFile = outputDir.resolve("$outPrefix$NEXT500_SEQS_FLANK_SUFFIX")
    peaksToFasta(summitsFile, twoBit, next500SeqsFile, methylBed, methylPercentThreshold,
            500 until 1000)
    fastaCenter(next500SeqsFile, 100, next500CenterSeqsFile, next500FlankSeqsFile)

    val memeTxtFile = memeDir.resolve(MEME_TXT_FILENAME)
    val next500CenterFimoDir = outputDir.resolve(CENTER_FIMO_DIR_SUFFIX)
    fimo(memeTxtFile, next500CenterSeqsFile, next500CenterFimoDir)

    val next500FlankFimoDir = outputDir.resolve(FLANK_FIMO_DIR_SUFFIX)
    fimo(memeTxtFile, next500FlankSeqsFile, next500FlankFimoDir)

    // Run FIMO against 100x random sequences from reference genome (with matching length and gc content)
    val randomSeqFile = outputDir.resolve("$outPrefix$SHUFFLED_SEQS_SUFFIX")
    val randomFimoDir = outputDir.resolve(SHUFFLED_FIMO_DIR_SUFFIX)
    randomSequences(twoBit, top500CenterSeqsFile, randomSeqFile, 100, chromSizes, 0.05,
            methylBed, methylPercentThreshold)
    fimo(memeTxtFile, randomSeqFile, randomFimoDir)

    // Run Motif Quality step
    val memeXmlFile = memeDir.resolve(MEME_XML_FILENAME)
    val outJsonFile = outputDir.resolve("$outPrefix$MOTIFS_JSON_SUFFIX")
    motifQuality(memeXmlFile, next500CenterFimoDir, randomFimoDir, next500FlankFimoDir, outJsonFile)
}

/**
 * Run occurrences file creation related steps
 */
fun CmdRunner.runOccurrencesSteps(outPrefix: String, cleanedPeaks: Path, twoBit: Path, outputDir: Path,
                                  methylBed: Path? = null, methylPercentThreshold: Int? = null) {
    // Create fasta file containing sequences for original input peaks file
    val originalPeaksFastaFile = outputDir.resolve("$outPrefix$SEQS_SUFFIX")
    peaksToFasta(cleanedPeaks, twoBit, originalPeaksFastaFile, methylBed, methylPercentThreshold, null)

    // Run FIMO against original peaks sequences
    val memeOutDir = outputDir.resolve("$outPrefix$MEME_DIR_SUFFIX")
    val memeTxtFile = memeOutDir.resolve(MEME_TXT_FILENAME)
    val originalPeaksFimoDir = outputDir.resolve("$outPrefix$FIMO_SUFFIX")
    fimo(memeTxtFile, originalPeaksFastaFile, originalPeaksFimoDir)

    // Convert FIMO Occurrences to custom Occurrences TSV with absolute positioned ranges
    val originalPeaksFimoTsv = originalPeaksFimoDir.resolve(FIMO_TSV_FILENAME)
    val occurrencesTsv = outputDir.resolve("$outPrefix$OCCURRENCES_SUFFIX")
    occurrencesTsv(originalPeaksFimoTsv, cleanedPeaks, occurrencesTsv)
}