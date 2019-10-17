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
    private val shuffleOutputsPerInput by option("--shuffle-outputs-per-input",
            help = "Number of shuffled sequences to fetch per input sequence")
            .int().default(100)
    private val shuffleGCTolerance by option("--shuffle-gc-tolerance",
            help = "Acceptable distance from input gc content for fetching output sequences during shuffle as percentage (as integer 0-100)")
            .int().default(10)
    private val methylBeds by option("--methyl-beds", help = "path to optional methylation state @CpG bed file")
            .path(exists = true)
            .multiple()
    private val methylPercentThreshold by option("--methyl-percent-threshold",
            help = "the percentage over which we will use a methylation site from the methylation bed file.")
            .int().default(0)

    override fun run() {
        val cmdRunner = DefaultCmdRunner()
        cmdRunner.runTask(peaks, twoBit, chromInfo, offset, outputDir, chrFilter.toSet(), shuffleOutputsPerInput,
                shuffleGCTolerance, methylBeds, methylPercentThreshold)
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
 * @param methylBeds methylated state @CpG files used for runs that create motifs with methyl base pairs
 * @param methylPercentThreshold the percentage over which we will use a methylation site
 */
fun CmdRunner.runTask(peaks: Path, twoBit: Path, chromInfo: Path, offset: Int, outputDir: Path,
                      chrFilter: Set<String>? = null, shuffleOutputsPerInput: Int, shuffleGCTolerance: Int,
                      methylBeds: List<Path> = listOf(), methylPercentThreshold: Int = 0) {
    log.info {
        """
        Running Meme task for
        peaks: $peaks
        twoBit: $twoBit
        chromInfo: $chromInfo
        offset: $offset
        outputDir: $outputDir
        chromFilter: $chrFilter
        methylBed: $methylBeds
        methylPercentThreshold: $methylPercentThreshold
        """.trimIndent()
    }
    val outPrefix = peaks.fileName.toString().split(".").first()
    val chromSizes = parseChromSizes(chromInfo)

    val methylData = if (methylBeds.isNotEmpty()) parseMethylBeds(methylBeds, methylPercentThreshold) else null

    // Rewrite peaks names, apply chrom filter and filter peaks without methylated states (if methyl bed is given)
    // Name rewrite is necessary because given peaks input may not include them
    log.info { "Creating cleaned peaks file..." }
    val cleanedPeaks = outputDir.resolve("$outPrefix$CLEANED_BED_SUFFIX")
    cleanPeaks(peaks, chrFilter, methylData, cleanedPeaks)
    log.info { "Cleaned peaks file complete!" }

    runMemeSteps(outPrefix, cleanedPeaks, twoBit, chromSizes, offset, outputDir, chrFilter, methylData)

    val summitsFile = outputDir.resolve("$outPrefix$SUMMITS_FILE_SUFFIX")
    val memeOutDir = outputDir.resolve("$outPrefix$MEME_DIR_SUFFIX")
    val top500CenterSeqsFile = outputDir.resolve("$outPrefix$TOP500_SEQS_CENTER_SUFFIX")
    runPostMemeSteps(outPrefix, summitsFile, memeOutDir, cleanedPeaks, top500CenterSeqsFile, twoBit,
            outputDir, chromSizes, shuffleOutputsPerInput, shuffleGCTolerance, methylData)
}

/**
 * Run Meme pre-processing and Meme steps
 */
fun CmdRunner.runMemeSteps(outPrefix: String, cleanedPeaks: Path, twoBit: Path, chromSizes: Map<String, Int>,
                           offset: Int, outputDir: Path, chrFilter: Set<String>? = null, methylData: MethylData? = null) {
    // Create summits file
    log.info { "Creating peak summits file..." }
    val summitsFile = outputDir.resolve("$outPrefix$SUMMITS_FILE_SUFFIX")
    summits(cleanedPeaks, chromSizes, 150, summitsFile, offset, chrFilter)
    log.info { "Peak summits File creation complete!" }

    // Run MEME on top 500 peaks
    log.info { "Creating fasta file from top 500 summits..." }
    val top500SeqsFile = outputDir.resolve("$outPrefix$TOP500_SEQS_SUFFIX")
    val top500CenterSeqsFile = outputDir.resolve("$outPrefix$TOP500_SEQS_CENTER_SUFFIX")
    peaksToFasta(summitsFile, twoBit, top500SeqsFile, methylData, 0 until 500)
    log.info { "Top 500 fasta file creation complete!" }

    log.info { "Centering top 500 sequences..." }
    fastaCenter(top500SeqsFile, 100, top500CenterSeqsFile)
    log.info { "Centering top 500 sequences complete!" }

    log.info { "Running meme on top 500 centered peaks..." }
    val memeOutDir = outputDir.resolve("$outPrefix$MEME_DIR_SUFFIX")
    val useMotifAlphabet = methylData != null
    meme(top500CenterSeqsFile, memeOutDir, useMotifAlphabet)
    log.info { "Top 500 centered peaks meme run complete!" }
}

// Length of sequences to use for peak centers, flanks, and shuffled regions
const val SEQUENCE_LENGTH = 100

/**
 * Run post-Meme quality related steps
 */
fun CmdRunner.runPostMemeSteps(outPrefix: String, summitsFile: Path, memeDir: Path, cleanedPeaks: Path,
                               top500CenterSeqsFile: Path, twoBit: Path, outputDir: Path, chromSizes: Map<String, Int>,
                               shuffleOutputsPerInput: Int, shuffleGCTolerance: Int, methylData: MethylData? = null) {
    // Run FIMO against peaks 501-1000 center and flanks
    log.info { "Generating 501-1000 peaks centers and flanks..." }
    val next500SeqsFile = outputDir.resolve("$outPrefix$NEXT500_SEQS_SUFFIX")
    val next500CenterSeqsFile = outputDir.resolve("$outPrefix$NEXT500_SEQS_CENTER_SUFFIX")
    val next500FlankSeqsFile = outputDir.resolve("$outPrefix$NEXT500_SEQS_FLANK_SUFFIX")
    peaksToFasta(summitsFile, twoBit, next500SeqsFile, methylData, 500 until 1000)
    fastaCenter(next500SeqsFile, SEQUENCE_LENGTH, next500CenterSeqsFile, next500FlankSeqsFile)
    log.info { "501-1000 peaks centers and flanks generation complete!" }

    log.info { "Running FIMO on 501-1000 peaks centers..." }
    val memeTxtFile = memeDir.resolve(MEME_TXT_FILENAME)
    val next500CenterFimoDir = outputDir.resolve("$outPrefix$CENTER_FIMO_DIR_SUFFIX")
    fimo(memeTxtFile, next500CenterSeqsFile, next500CenterFimoDir)
    log.info { "FIMO run on 501-1000 peaks centers complete!" }

    log.info { "Running FIMO on 501-1000 peaks flanks..." }
    val next500FlankFimoDir = outputDir.resolve(FLANK_FIMO_DIR_SUFFIX)
    fimo(memeTxtFile, next500FlankSeqsFile, next500FlankFimoDir)
    log.info { "FIMO run on 501-1000 peaks flanks complete!" }

    // Run FIMO against 100x random sequences from reference genome (with matching length and gc content)
    val randomSeqFile = outputDir.resolve("$outPrefix$SHUFFLED_SEQS_SUFFIX")
    val randomFimoDir = outputDir.resolve(SHUFFLED_FIMO_DIR_SUFFIX)
    log.info { "Generating Shuffled sequences..." }
    randomSequences(twoBit, top500CenterSeqsFile, randomSeqFile, shuffleOutputsPerInput, chromSizes, SEQUENCE_LENGTH,
            shuffleGCTolerance, methylData)
    log.info { "Shuffled sequence generation complete!" }
    log.info { "Running FIMO on shuffled sequences..." }
    fimo(memeTxtFile, randomSeqFile, randomFimoDir)
    log.info { "FIMO run on shuffled sequences complete!" }

    // Create fasta file containing sequences for original input peaks file
    log.info { "Creating fasta from original cleaned peaks..." }
    val originalPeaksFastaFile = outputDir.resolve("$outPrefix$SEQS_SUFFIX")
    peaksToFasta(cleanedPeaks, twoBit, originalPeaksFastaFile, methylData, null)
    log.info { "Fasta from original cleaned peaks complete!" }

    // Run FIMO against original peaks sequences
    log.info { "Running FIMO on original peaks fasta..." }
    val originalPeaksFimoDir = outputDir.resolve("$outPrefix$FIMO_SUFFIX")
    fimo(memeTxtFile, originalPeaksFastaFile, originalPeaksFimoDir)
    log.info { "FIMO run on original peaks fasta complete!" }

    // Convert FIMO Occurrences to custom Occurrences TSV with absolute positioned ranges
    log.info { "Creation occurrences.tsv..." }
    val originalPeaksFimoTsv = originalPeaksFimoDir.resolve(FIMO_TSV_FILENAME)
    val occurrencesTsv = outputDir.resolve("$outPrefix$OCCURRENCES_SUFFIX")
    occurrencesTsv(originalPeaksFimoTsv, cleanedPeaks, occurrencesTsv)
    log.info { "occurrences.tsv creation complete!" }

    // Create motifs json
    log.info { "Creating motifs.json file..." }
    val memeXmlFile = memeDir.resolve(MEME_XML_FILENAME)
    val outJsonFile = outputDir.resolve("$outPrefix$MOTIFS_JSON_SUFFIX")
    motifJson(memeXmlFile, originalPeaksFimoDir, next500CenterFimoDir, randomFimoDir, next500FlankFimoDir, outJsonFile)
    log.info { "motifs.json file creation complete!" }
}
