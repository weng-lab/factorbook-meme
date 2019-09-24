package util

import java.nio.file.*
import java.util.*
import java.util.zip.GZIPInputStream

data class MethylData (private val data: Map<String, List<Int>>) {

    fun containsValueInRange(chrom: String, range: IntRange): Boolean {
        val chromValues = this.data.getOrElse(chrom) { return false }
        val searchResult = chromValues.binarySearch {
            when {
                it < range.first -> -1
                it > range.last -> 1
                else -> 0
            }
        }
        return searchResult > 0
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
                    else -> throw Exception("Invalid methylation state found at location: $chr:$bp")
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
        val rangeFirstSearchResult = chromValues.binarySearch(range.first)
        var subListStart = rangeFirstSearchResult
        if (rangeFirstSearchResult < 0) {
            // If not found, binarySearch returns the negative of the "insertion point" or where the value
            // would be inserted to maintain sort order
            subListStart = -rangeFirstSearchResult
            if (subListStart >= chromValues.size || chromValues[subListStart] > range.last) return listOf()
        }

        val rangeLastSearchResult = chromValues.binarySearch(range.last)
        val subListEnd =
                if (rangeLastSearchResult < 0) -rangeLastSearchResult - 1
                else rangeLastSearchResult
        return chromValues.subList(subListStart, subListEnd)
    }
}

fun parseMethylBed(methylBed: Path, methylPercentThreshold: Int?): MethylData {
    val data = mutableMapOf<String, List<Int>>()
    val rawInputStream = Files.newInputStream(methylBed)
    val inputStream =
            if (methylBed.toString().endsWith(".gz")) GZIPInputStream(rawInputStream)
            else rawInputStream
    inputStream.reader().forEachLine { line ->
        val lineParts = line.trim().split("\t")
        val chrom = lineParts[0]
        val start = lineParts[1].toInt()
        val methylPercent = lineParts[10].toInt()
        if (methylPercentThreshold != null && methylPercent < methylPercentThreshold) return@forEachLine
        val chromValues = data.getOrPut(chrom) { mutableListOf() } as MutableList<Int>
        chromValues += start
    }

    for (chromValues in data.values) {
        (chromValues as MutableList<Int>).sort()
    }
    return MethylData(data)
}