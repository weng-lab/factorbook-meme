package step

import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.apache.commons.math3.special.Erf.erf
import org.w3c.dom.Element
import java.nio.file.*
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.sqrt
import com.squareup.moshi.Types.newParameterizedType


data class OutputMotif(
        val pwm: List<Map<Char, Double>>,
        // Ratio of occurrences for this motif to total regions
        @Json(name = "occurrences_ratio") val occurrencesRatio: Double,
        // Control data from flank sequences
        @Json(name = "flank_control_data") val flankControlData: MotifControlData,
        // Control data from shuffled sequences
        @Json(name = "shuffled_control_data") val shuffledControlData: MotifControlData
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
 * @param peaksFimoDir path to results of FIMO run against 501-1000 peaks centers.
 * @param shuffledFimoDir path to results of FIMO run against shuffled sequences pulled randomly from reference genome.
 * @param flankFimoDir path to results of FIMO run againt 501-1000 peaks flanks.
 * @param outJson path for output json file.
 */
fun motifQuality(memeXml: Path, peaksFimoDir: Path, shuffledFimoDir: Path, flankFimoDir: Path, outJson: Path) {
    val memeMotifs = parseMotifs(memeXml)
    val peaksOccurrenceRatios = motifOccurrencesRatios(peaksFimoDir)
    val flankOccurrenceRatios = motifOccurrencesRatios(flankFimoDir)
    val shuffledOccurrenceRatios = motifOccurrencesRatios(shuffledFimoDir)

    val outputMotifs = mutableListOf<OutputMotif>()
    for (memeMotif in memeMotifs) {
        val motifName = memeMotif.name
        val peaksOccurrenceRatioData = peaksOccurrenceRatios.getValue(motifName)

        val flankOccurrenceRatioData = flankOccurrenceRatios.getValue(motifName)
        val flankZScore = compareOccurrenceProportions(peaksOccurrenceRatioData, flankOccurrenceRatioData)
        val flankPValue = zScoreToPValue(flankZScore)

        val shuffledOccurrenceRatio = shuffledOccurrenceRatios.getValue(motifName)
        val shuffledZScore = compareOccurrenceProportions(peaksOccurrenceRatioData, shuffledOccurrenceRatio)
        val shuffledPValue = zScoreToPValue(shuffledZScore)

        outputMotifs += OutputMotif(
                pwm = memeMotif.pwm,
                occurrencesRatio = peaksOccurrenceRatios.getValue(motifName).ratio,
                flankControlData = MotifControlData(flankOccurrenceRatioData.ratio, flankZScore, flankPValue),
                shuffledControlData = MotifControlData(shuffledOccurrenceRatio.ratio, shuffledZScore, shuffledPValue)
        )
    }
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val adapter = moshi.adapter<List<OutputMotif>>(
            newParameterizedType(List::class.java, OutputMotif::class.java))
    val outputJsonText = adapter.toJson(outputMotifs)
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

    return (testRatioData.ratio - controlRatioData.ratio) /
            sqrt(combinedRatio * (1 - combinedRatio) *
                    ((1 / testRatioData.sourceSequences.toDouble()) + (1 / controlRatioData.sourceSequences.toDouble())))
}

data class MemeMotif(
        val name: String,
        val pwm: List<Map<Char, Double>>
)

/**
 * Parse Basic motif data from meme xml file.
 */
fun parseMotifs(memeXml: Path): List<MemeMotif> {
    val motifs = mutableListOf<MemeMotif>()
    val xmlDocBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val doc = xmlDocBuilder.parse(memeXml.toFile())
    val memeRoot = doc.documentElement
    val motifNodes = memeRoot.getElementsByTagName("motif")
    for (i in 0 until motifNodes.length) {
        val motifEl = motifNodes.item(i) as Element
        val motifName = motifEl.getAttribute("name")
        val probabilitiesEl = motifEl.getElementsByTagName("probabilities").item(0) as Element
        val matrixEl = probabilitiesEl.getElementsByTagName("alphabet_matrix").item(0) as Element
        val arrayNodes = matrixEl.getElementsByTagName("alphabet_array")
        val pwm = mutableListOf<Map<Char, Double>>()
        for (j in 0 until arrayNodes.length) {
            val arrayEl = arrayNodes.item(j) as Element
            val valueNodes = arrayEl.getElementsByTagName("value")
            val pwmItem = mutableMapOf<Char, Double>()
            for (k in 0 until valueNodes.length) {
                val valueEl = valueNodes.item(k) as Element
                val letter = valueEl.getAttribute("letter_id").toCharArray()[0]
                val probability = valueEl.textContent.toDouble()
                pwmItem[letter] = probability
            }
            pwm += pwmItem
        }
        motifs += MemeMotif(motifName, pwm)
    }
    return motifs
}

data class OccurrenceRatioData(val ratio: Double, val occurrences: Int, val sourceSequences: Int)

/**
 * Parse files in fimo directory to get ratios of occurrences to total sequences they were pulled from
 *
 * @return Map containing occurrence ratios by Meme's motif "name"
 */
fun motifOccurrencesRatios(fimoDir: Path): Map<String, OccurrenceRatioData> {
    val fimoXml = fimoDir.resolve("fimo.xml")
    val numSequences = parseNumSequences(fimoXml)

    val fimoTsv = fimoDir.resolve("fimo.tsv")
    val occurrenceCounts = motifOccurrencesCounts(fimoTsv)

    return occurrenceCounts
            .map { (motif, count) ->
                motif to OccurrenceRatioData(count.toDouble() / numSequences, count, numSequences)
            }
            .toMap()
}

/**
 * Parse the number of sequences from fimo run in fimo xml
 */
fun parseNumSequences(fimoXml: Path): Int {
    val xmlDocBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val doc = xmlDocBuilder.parse(fimoXml.toFile())
    val fimoRoot = doc.documentElement
    val sequenceDataElement = fimoRoot.getElementsByTagName("sequence-data").item(0) as Element
    return sequenceDataElement.getAttribute("num-sequences").toInt()
}

/**
 * Pull the number of occurrences per motif from a fimo run by parsing fimo tsv file.
 * Only one occurrence will be counted per sequence.
 *
 * @return Map containing number of occurrences by Meme's motif "name"
 */
fun motifOccurrencesCounts(fimoTsv: Path): Map<String, Int> {
    val sequencesPerMotif = mutableMapOf<String, MutableSet<String>>()
    Files.newBufferedReader(fimoTsv).use { reader ->
        reader.forEachLine { line ->
            // Skip header, comment, and empty lines
            if (line.startsWith("motif_id") || line.startsWith("#") || line.isBlank()) return@forEachLine
            val motifName = line.split("\\s".toRegex())[0]
            val sequence = line.split("\\s".toRegex())[2]
            if(!sequencesPerMotif.containsKey(motifName)) sequencesPerMotif[motifName] = mutableSetOf()
            sequencesPerMotif.getValue(motifName).add(sequence)
        }
    }
    return sequencesPerMotif.mapValues { (_, sequences) -> sequences.size }
}