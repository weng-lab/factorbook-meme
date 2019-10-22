package step

import mu.KotlinLogging
import org.biojava.nbio.genome.parsers.twobit.TwoBitParser
import util.*
import java.nio.file.*


private val log = KotlinLogging.logger {}

const val RANDOM_PREFIX = "Random"

/**
 * Selects random regions from the given twoBit file that match gc content and length of sequences
 * from a given fasta file. If a methyl state bed file is provided, only regions near at least one methylated
 * base pair will be used.
 *
 * @param twoBit The twoBit file
 * @param inputFasta The input fasta file we're matching
 * @param outputFasta The fasta file to output our random sequences to
 * @param outputsPerInput the number of matching output sequences to find per input sequence
 * @param chromosomeSizes chromosome size information for given twoBit file
 * @param gcContentTolerance max acceptable distance from gcContent that will still be accepted (as percentage)
 * @param sampleRatio ratio of samples to outputs to take to attempt to match against
 * @param methylData useful data parsed from methyl beds
 */
fun randomSequences(twoBit: Path,
                    inputFasta: Path,
                    outputFasta: Path,
                    outputsPerInput: Int,
                    chromosomeSizes: Map<String, Int>,
                    sequenceLength: Int,
                    gcContentTolerance: Int,
                    methylData: MethylData? = null) {

    data class RandomSequenceTracker(val gcContent: Int, val outputs: MutableList<String> = mutableListOf(), var addAttempts: Int = 0)
    val outputsTrackers = parseFastaSequences(inputFasta)
            .map { RandomSequenceTracker(sequenceGCContent(it)) }
            .sortedBy { it.gcContent }

    iterateAssemblySequences(twoBit, chromosomeSizes, sequenceLength) { chrom, seqRange, rawSequence ->
        var sequence = rawSequence

        if(methylData != null) {
            // If we're doing a methylated motif analysis, check make sure there is a methylated site within
            // 500 base pairs of the center of the random range
            val randomRangeCenter = (seqRange.first + seqRange.last) / 2
            val rangeToCheck = randomRangeCenter - 500 .. randomRangeCenter + 500
            if(!methylData.containsValueInRange(chrom, rangeToCheck)) return@iterateAssemblySequences
            // Replace methylated bases
            sequence = methylData.replaceBases(sequence, chrom, seqRange)
        }

        val sequenceGCContent = sequenceGCContent(sequence)
        val matchingTrackers = outputsTrackers
                .rangeBinarySearch(sequenceGCContent - gcContentTolerance .. sequenceGCContent + gcContentTolerance) { it.gcContent }
        if(matchingTrackers.isEmpty()) return@iterateAssemblySequences

        // First, add to any input -> outputs trackers that are not already full
        val notFullTracker = matchingTrackers.firstOrNull { it.outputs.size < outputsPerInput }
        if (notFullTracker != null) {
            notFullTracker.outputs += sequence
            return@iterateAssemblySequences
        }

        val tracker = matchingTrackers.random()
        tracker.addAttempts++
        // Reservoir sampling algorithm
        val randomOutputIndex = (0 until outputsPerInput + tracker.addAttempts).random()
        if (randomOutputIndex < outputsPerInput) tracker.outputs[randomOutputIndex] = sequence
    }

    val incomplete = outputsTrackers.filter { it.outputs.size < outputsPerInput }
    if (incomplete.isNotEmpty()) {
        val missing = incomplete.map { outputsPerInput - it.outputs.size }.sum()
        log.warn { "Could not find enough sequences matching GC Content of inputs. Missing sequences: $missing" }
    }

    val outputSequences = outputsTrackers.flatMap { it.outputs }

    Files.createDirectories(outputFasta.parent)
    Files.newBufferedWriter(outputFasta).use { writer ->
        for ((index, sequence) in outputSequences.withIndex()) {
            writer.write(">${RANDOM_PREFIX}_$index\n")
            writer.write("$sequence\n")
        }
    }
}

private fun iterateAssemblySequences(twoBit: Path, chromosomeSizes: Map<String, Int>, sequenceLength: Int,
                                     handle: (chrom: String, seqRange: IntRange, sequence: String) -> Unit) {
    for ((chrom, chromLen) in chromosomeSizes) {
        val parser = TwoBitParser(twoBit.toFile())
        parser.setCurrentSequence(chrom)
        parser.bufferedReader().use { reader ->
            var loc = 0
            while (loc + sequenceLength < chromLen) {
                val seqRange = loc until (loc + sequenceLength)
                val sequenceBuffer = CharArray(sequenceLength)
                try {
                    val readLen = reader.read(sequenceBuffer)
                    if (readLen < sequenceLength) break
                } catch (e: Exception) {
                    log.error { "Error loading fragment from two-bit file $twoBit at $chrom:${seqRange.first}-${seqRange.last}" }
                    continue
                } finally {
                    loc += sequenceLength
                }
                val sequence = String(sequenceBuffer)
                handle(chrom, seqRange, sequence)
            }
        }
    }

}

private fun sequenceGCContent(sequence: String): Int =
        ((sequence.count { "gcmw".contains(it, ignoreCase = true) } / sequence.length.toDouble()) * 100).toInt()

private fun parseFastaSequences(fasta: Path): List<String> {
    val sequences = mutableListOf<String>()
    Files.newBufferedReader(fasta).use { reader ->
        var currentSeq: String? = null
        reader.forEachLine { line ->
            if (line.startsWith(">") || line.isBlank()) {
                if (currentSeq != null) sequences += currentSeq!!
                currentSeq = null
            } else {
                if (currentSeq == null) {
                    currentSeq = line
                } else {
                    currentSeq += line
                }
            }
        }
        if (currentSeq != null) sequences += currentSeq!!
    }
    return sequences
}
