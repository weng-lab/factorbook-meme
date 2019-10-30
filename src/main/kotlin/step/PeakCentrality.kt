package step

import util.*
import java.nio.file.*
import kotlin.math.*

/**
 * Calculates "centrality" for each motif
 *
 * For each motif, we take every occurrence and find the distance (sign included) between the center of
 * the occurrence and the peak summit, and we create a normalized distribution.
 *
 * @return a map of motifs to distributions, where distributions are distances to percentage of occurrences
 * at that distance from peak summits.
 */
fun peakCentrality(fimoTsv: Path, peaksBed: Path): Map<String, Map<Int, Double>> {
    val peakSummits = mutableMapOf<String, Int>()
    readPeaksFile(peaksBed) { row ->
        peakSummits[row.name] = row.peak
    }

    val counts = mutableMapOf<String, Map<Int, Int>>()
    readFimoTsv(fimoTsv) { row ->
        val occurrenceCenter = (row.relativeStart + row.relativeEnd) / 2.0
        val occurrenceCenterRounded =
                if (row.strand == "-") ceil(occurrenceCenter).toInt()
                else floor(occurrenceCenter).toInt()
        val peak = peakSummits.getValue(row.peakId)
        var distanceFromSummit = peak - occurrenceCenterRounded
        if (row.strand == "-") distanceFromSummit = -distanceFromSummit
        val motifCounts = counts.getOrPut(row.motifId) { mutableMapOf() } as MutableMap
        motifCounts[distanceFromSummit] = (motifCounts[distanceFromSummit] ?: 0) + 1
    }

    // Normalize / sort for each motif
    return counts.mapValues { (_, motifCounts) ->
        val sum = motifCounts.values.sum().toDouble()
        motifCounts.mapValues { (_, count) -> count / sum }.toSortedMap()
    }
}