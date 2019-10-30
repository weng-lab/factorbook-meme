package step

import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.apache.commons.math3.special.Erf.erf
import java.nio.file.*
import kotlin.math.sqrt
import util.*

data class MotifData(
        val motifs: List<OutputMotif>,
        // background frequencies of the centered top 500 peaks data used to run MEME
        val backgroundFrequencies: Map<Char, Double>
)

data class OutputMotif(
        val name: String,
        val pwm: List<Map<Char, Double>>,
        // MEME "e_value" score
        @Json(name = "e_value") val eValue: Double,
        // the number of occurrences found by MEME in the top 500
        val sites: Int,
        // Number of original peaks
        @Json(name = "original_peaks") val originalPeaks: Int,
        // Number of occurrences in original peaks
        @Json(name = "original_peaks_occurrences") val originalPeaksOccurrences: Int,
        // Ratio of occurrences to sequences in top 501-1000 for this motif
        @Json(name = "lesser_peaks_occurrences_ratio") val lesserPeaksOccurrencesRatio: Double,
        // Control data from flank sequences
        @Json(name = "flank_control_data") val flankControlData: MotifControlData,
        // Control data from shuffled sequences
        @Json(name = "shuffled_control_data") val shuffledControlData: MotifControlData,
        // Peak Centrality. The distribution of occurrences' distance from peak summits.
        @Json(name = "peak_centrality") val peakCentrality: Map<Int, Double>
)

data class MotifControlData(
        @Json(name = "occurrences_ratio") val occurrencesRatio: Double,
        @Json(name = "z_score") val zScore: Double,
        @Json(name = "p_value") val pValue: Double
)

/**
 * Run our final step that puts together our meme output quality scores we calculate based our fimo results.
 *
 * @param memeXml path to results of MEME run. This contains our motifs for top 500 peaks, centered.
 * @param origPeaksFimoDir path to results of FIMO run against original (cleaned) peaks.
 * @param next500FimoDir path to results of FIMO run against 501-1000 peaks centers.
 * @param shuffledFimoDir path to results of FIMO run against shuffled sequences pulled randomly from reference genome.
 * @param flankFimoDir path to results of FIMO run againt 501-1000 peaks flanks.
 * @param outJson path for output json file.
 */
fun motifJson(memeXml: Path, origPeaksFimoDir: Path, next500FimoDir: Path, shuffledFimoDir: Path,
                 flankFimoDir: Path, peaksBed: Path, outJson: Path) {
    val memeData = parseMotifs(memeXml)
    val memeMotifNames = memeData.motifs.map { it.name }

    val origPeaksFimoXml = origPeaksFimoDir.resolve(FIMO_XML_FILENAME)
    val origPeaks = parseNumSequences(origPeaksFimoXml)

    val origPeaksFimoTsv = origPeaksFimoDir.resolve(FIMO_TSV_FILENAME)
    val origPeaksOccurrences = motifOccurrencesCounts(origPeaksFimoTsv, memeMotifNames)

    val peakCentrality = peakCentrality(origPeaksFimoTsv, peaksBed)

    val lesserPeaksOccurrenceRatios = motifOccurrencesRatios(next500FimoDir, memeMotifNames)
    val flankOccurrenceRatios = motifOccurrencesRatios(flankFimoDir, memeMotifNames)
    val shuffledOccurrenceRatios = motifOccurrencesRatios(shuffledFimoDir, memeMotifNames)

    val outputMotifs = mutableListOf<OutputMotif>()
    for (memeMotif in memeData.motifs) {
        val motifName = memeMotif.name
        val lesserPeaksOccurrenceRatioData = lesserPeaksOccurrenceRatios.getValue(motifName)

        val flankOccurrenceRatioData = flankOccurrenceRatios.getValue(motifName)
        val flankZScore = compareOccurrenceProportions(lesserPeaksOccurrenceRatioData, flankOccurrenceRatioData)
        val flankPValue = zScoreToPValue(flankZScore)

        val shuffledOccurrenceRatio = shuffledOccurrenceRatios.getValue(motifName)
        val shuffledZScore = compareOccurrenceProportions(lesserPeaksOccurrenceRatioData, shuffledOccurrenceRatio)
        val shuffledPValue = zScoreToPValue(shuffledZScore)

        outputMotifs += OutputMotif(
                name = motifName,
                pwm = memeMotif.pwm,
                eValue = memeMotif.eValue,
                sites = memeMotif.sites,
                originalPeaks = origPeaks,
                originalPeaksOccurrences = origPeaksOccurrences.getValue(motifName),
                lesserPeaksOccurrencesRatio = lesserPeaksOccurrenceRatios.getValue(motifName).ratio,
                flankControlData = MotifControlData(flankOccurrenceRatioData.ratio, flankZScore, flankPValue),
                shuffledControlData = MotifControlData(shuffledOccurrenceRatio.ratio, shuffledZScore, shuffledPValue),
                peakCentrality = peakCentrality.getValue(motifName)
        )
    }
    val motifData = MotifData(outputMotifs, memeData.letterFrequencies)

    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val adapter = moshi.adapter(MotifData::class.java).indent("  ")
    val outputJsonText = adapter.toJson(motifData)
    Files.createDirectories(outJson.parent)
    Files.newBufferedWriter(outJson, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            .use { writer -> writer.write(outputJsonText) }
}

fun zScoreToPValue(zScore: Double): Double {
    val x = (1 + erf(zScore / sqrt(2.0))) / 2
    return if (x > 0.5) 1 - x else x
}

/**
 * Calculate zScore occurrence ratios of test vs control data
 */
fun compareOccurrenceProportions(testRatioData: OccurrenceRatioData,
                                         controlRatioData: OccurrenceRatioData): Double {
    val combinedRatio = (testRatioData.occurrences + controlRatioData.occurrences).toDouble() /
            (testRatioData.sourceSequences + controlRatioData.sourceSequences)

    if (combinedRatio == 0.0) return 0.0
    return (testRatioData.ratio - controlRatioData.ratio) /
            sqrt(combinedRatio * (1 - combinedRatio) *
                    ((1 / testRatioData.sourceSequences.toDouble()) + (1 / controlRatioData.sourceSequences.toDouble())))
}

data class OccurrenceRatioData(val ratio: Double, val occurrences: Int, val sourceSequences: Int)

/**
 * Parse files in fimo directory to get ratios of occurrences to total sequences they were pulled from
 *
 * @return Map containing occurrence ratios by Meme's motif "name"
 */
fun motifOccurrencesRatios(fimoDir: Path, motifNames: List<String>): Map<String, OccurrenceRatioData> {
    val fimoXml = fimoDir.resolve(FIMO_XML_FILENAME)
    val numSequences = parseNumSequences(fimoXml)

    val fimoTsv = fimoDir.resolve(FIMO_TSV_FILENAME)
    val occurrenceCounts = motifOccurrencesCounts(fimoTsv, motifNames)

    return motifNames
            .map { motif ->
                val count = occurrenceCounts.getOrDefault(motif, 0)
                motif to OccurrenceRatioData(count.toDouble() / numSequences, count, numSequences)
            }
            .toMap()
}

/**
 * Pull the number of occurrences per motif from a fimo run by parsing fimo tsv file.
 * Only one occurrence will be counted per sequence.
 *
 * @return Map containing number of occurrences by Meme's motif "name"
 */
fun motifOccurrencesCounts(fimoTsv: Path, motifNames: List<String>): Map<String, Int> {
    val sequencesPerMotif =
            motifNames.map { it to mutableSetOf<String>() }.toMap() as MutableMap
    readFimoTsv(fimoTsv) { row ->
        sequencesPerMotif.getValue(row.motifId).add(row.peakId)
    }
    return sequencesPerMotif.mapValues { (_, sequences) -> sequences.size }
}