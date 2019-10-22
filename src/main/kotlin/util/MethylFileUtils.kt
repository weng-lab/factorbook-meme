package util

import mu.KotlinLogging
import org.eclipse.collections.impl.map.mutable.primitive.*
import java.nio.file.*
import java.util.zip.GZIPInputStream
import kotlin.math.max


private val log = KotlinLogging.logger {}

data class MethylData (private val data: Map<String, List<Int>>) {

    fun containsValueInRange(chrom: String, range: IntRange): Boolean {
        val chromValues = this.data.getOrElse(chrom) { return false }
        val searchResult = chromValues.binarySearch {
            when {
                it < range.first - 1 -> -1
                it > range.last -> 1
                else -> 0
            }
        }
        return searchResult >= 0
    }

    /**
     * Replace all CG at methylated locations with MW for the given segment
     */
    fun replaceBases(segment: String, chr: String, range: IntRange): String {
        val methylBPs = this.getMethylBPsInRange(chr, range)
        val methylSegment = StringBuilder()
        for ((index, char) in segment.withIndex()) {
            val bp = range.first + index
            val charToWrite = if (methylBPs.contains(bp)) {
                when(char) {
                    'c' -> 'm'
                    'C' -> 'M'
                    'g' -> 'w'
                    'G' -> 'W'
                    else -> {
                        log.warn { "Invalid methylation state found at location: $chr:$bp" }
                        char
                    }
                }
            } else char
            methylSegment.append(charToWrite)
        }
        return methylSegment.toString()
    }

    /**
     * find all methylated state base pairs within a given range
     *
     * @param chrom chromosome to search for the methylated states on
     * @param range the range to search within
     * @return a list of base pairs within the search range
     */
    private fun getMethylBPsInRange(chrom: String, range: IntRange): List<Int> {
        val chromValues = this.data.getOrElse(chrom) { return listOf() }
        return chromValues.rangeBinarySearch(range)
                .flatMap { listOf(it, it+1) }
                .filter { range.contains(it) }
    }
}

fun parseMethylBeds(methylBeds: List<Path>, methylPercentThreshold: Int): MethylData {
    val data = mutableMapOf<String, List<Int>>()

    if (methylBeds.size > 1) {
        // Map of Chromosomes -> Base Pair Indexes -> average methyl percents from files
        val allBedValues = mutableMapOf<String, IntFloatHashMap>()

        for ((i, methylBed) in methylBeds.withIndex()) {
            readMethylBed(methylBed) { chrom, start, methylPercent ->
                if (methylPercent == 0) return@readMethylBed
                val chromValues = allBedValues.getOrPut(chrom) { IntFloatHashMap() }
                val newAvgValue = ((chromValues[start] * i) + (methylPercent)) / (i + 1)
                if (newAvgValue > 0) chromValues.put(start, newAvgValue)
            }
        }

        val allBedValuesIter = allBedValues.iterator()
        allBedValuesIter.forEach { (chrom, bpsToMethylValues) ->
            val chromData = data.getOrPut(chrom) { mutableListOf() } as MutableList
            for (entry in bpsToMethylValues.keyValuesView()) {
                val bp = entry.one
                val methylValueAvg = entry.two
                if (methylValueAvg >= methylPercentThreshold) chromData += bp
            }
            allBedValuesIter.remove()
            chromData.sort()
        }
    } else {
        val methylBed = methylBeds[0]
        readMethylBed(methylBed) { chrom, start, methylPercent ->
            val chromData = data.getOrPut(chrom) { mutableListOf() } as MutableList
            if (methylPercent >= methylPercentThreshold) chromData += start
        }
        for ((_, bpsToMethylValues) in data) {
            (bpsToMethylValues as MutableList).sort()
        }
    }

    return MethylData(data)
}

/**
 * Iterates through methyl bed file and runs the "handle" file for each pair of lines meant to
 * represent an "MW" pair with a score.
 */
fun readMethylBed(methylBed: Path, handle: (chrom: String, start: Int, pairMethylPercent: Int) -> Unit) {
    val rawInputStream = Files.newInputStream(methylBed)
    val inputStream =
            if (methylBed.toString().endsWith(".gz")) GZIPInputStream(rawInputStream)
            else rawInputStream
    var pairStarted = false
    var firstBP = -1
    var firstMethylPercent = -1
    inputStream.reader().forEachLine { line ->
        val lineParts = line.trim().split("\t")
        val chrom = lineParts[0]
        val bp = lineParts[1].toInt()
        val methylPercent = lineParts[10].toInt()
        if (pairStarted && bp - firstBP == 1){
            pairStarted = false
            handle(chrom, firstBP, max(firstMethylPercent, methylPercent))
        } else {
            pairStarted = true
            firstBP = bp
            firstMethylPercent = methylPercent
        }
    }
}