package step

import util.*
import java.nio.file.Path

/**
 * Rewrites narrowPeaks files with new names.
 * Optionally filters out chromosomes that do not match given filter
 * If methylation state data file is given, filters out peaks that don't intersect with any bases in it
 *
 * @param peaksBed the peaks file
 * @param chrFilter Optional set of chromsomes to filter against. Anything not included is filtered out.
 * @param methylData the methyl bed file
 * @param out the file to the filtered peaks results to
 */
fun cleanPeaks(peaksBed: Path, chrFilter: Set<String>?, methylData: MethylData?, out: Path) {
    val filteredPeaks = mutableListOf<PeaksRow>()
    var peakCount = 0
    readPeaksFile(peaksBed) { row ->
        if (!excludedByChrFilter(row, chrFilter) && !excludedByMethyl(row, methylData) ) {
            filteredPeaks += row.copy(name = "peak_${peakCount++}")
        }
    }
    writePeaksFile(out, filteredPeaks)
}

private fun excludedByChrFilter(row: PeaksRow, chrFilter: Set<String>?) =
        chrFilter != null && chrFilter.contains(row.chrom)
private fun excludedByMethyl(row: PeaksRow, methylData: MethylData?) =
        methylData != null && !methylData.containsValueInRange(row.chrom, row.chromStart .. row.chromEnd)
