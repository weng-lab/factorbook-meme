package step

import mu.KotlinLogging
import util.*
import java.nio.file.Path
import kotlin.system.exitProcess

private val log = KotlinLogging.logger {}

/**
 * Rewrites narrowPeaks files with new names.
 * Optionally filters out chromosomes that do not match given filter
 * If methylation state data file is given, filters out peaks that don't intersect with any bases in it
 *
 * @param peaksBed the peaks file
 * @param chrFilter Optional set of chromosomes to filter against. Anything not included is filtered out.
 * @param methylData the methyl bed file
 * @param out the file to the filtered peaks results to
 */
fun cleanPeaks(peaksBed: Path, chrFilter: Set<String>?, methylData: MethylData?, out: Path, chrInclusion: Set<String>?) {
    val filteredPeaks = mutableListOf<PeaksRow>()
    var peakCount = 0
    readPeaksFile(peaksBed) { row ->
        if (!excludedByChrFilter(row, chrFilter) && excludedByChrFilter(row, chrInclusion) && !excludedByMethyl(row, methylData) ) {
            filteredPeaks += row.copy(name = "peak_${peakCount++}")
        }
    }

    if (filteredPeaks.size < 1000) {
        log.error { "Not enough usable peaks. ${filteredPeaks.size} available out of 1000 required. Stopping with exit code 0." }
        exitProcess(0)
    }
    writePeaksFile(out, filteredPeaks)
}

private fun excludedByChrFilter(row: PeaksRow, chrFilter: Set<String>?) =
        chrFilter != null && chrFilter.size > 0 && chrFilter.contains(row.chrom)

private fun excludedByMethyl(row: PeaksRow, methylData: MethylData?) =
        methylData != null && !methylData.containsValueInRange(row.chrom, row.chromStart .. row.chromEnd)
