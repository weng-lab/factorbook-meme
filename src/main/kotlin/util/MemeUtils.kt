package util

import org.w3c.dom.Element
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory

data class MemeData(val letterFrequencies: Map<Char, Double>, val motifs: List<MemeMotif>)

data class MemeMotif(
        val name: String,
        val eValue: Double,
        val sites: Int,
        val pwm: List<Map<Char, Double>>
)

/**
 * Parse Basic motif data from meme xml file.
 */
fun parseMotifs(memeXml: Path): MemeData {
    val xmlDocBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val doc = xmlDocBuilder.parse(memeXml.toFile())
    val memeRoot = doc.documentElement

    val trainingSetEl = memeRoot.getElementsByTagName("training_set").item(0) as Element
    val letterFrequenciesEl = trainingSetEl.getElementsByTagName("letter_frequencies").item(0) as Element
    val lfArrayEl = letterFrequenciesEl.getElementsByTagName("alphabet_array").item(0) as Element
    val letterFrequencies = parseMemeAlphabetArray(lfArrayEl)

    val motifs = mutableListOf<MemeMotif>()
    val motifNodes = memeRoot.getElementsByTagName("motif")
    for (i in 0 until motifNodes.length) {
        val motifEl = motifNodes.item(i) as Element
        val motifName = motifEl.getAttribute("name")
        val eValue = motifEl.getAttribute("e_value").toDouble()
        val sites = motifEl.getAttribute("sites").toInt()
        val probabilitiesEl = motifEl.getElementsByTagName("probabilities").item(0) as Element
        val matrixEl = probabilitiesEl.getElementsByTagName("alphabet_matrix").item(0) as Element
        val arrayNodes = matrixEl.getElementsByTagName("alphabet_array")
        val pwm = mutableListOf<Map<Char, Double>>()
        for (j in 0 until arrayNodes.length) {
            pwm += parseMemeAlphabetArray(arrayNodes.item(j) as Element)
        }
        motifs += MemeMotif(motifName, eValue, sites, pwm)
    }

    return MemeData(letterFrequencies, motifs)
}

private fun parseMemeAlphabetArray(arrayEl: Element): Map<Char, Double> {
    val parsed = mutableMapOf<Char, Double>()
    val valueNodes = arrayEl.getElementsByTagName("value")
    for (k in 0 until valueNodes.length) {
        val valueEl = valueNodes.item(k) as Element
        val letter = valueEl.getAttribute("letter_id").toCharArray()[0]
        val value = valueEl.textContent.toDouble()
        parsed[letter] = value
    }
    return parsed
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