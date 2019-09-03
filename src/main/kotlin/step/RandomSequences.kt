package step

import org.biojava.nbio.genome.parsers.twobit.TwoBitParser
import java.nio.file.Files
import java.nio.file.Path


const val RANDOM_PREFIX = "Random"

private data class Sequence(val seq: String, val chromosome: String, val range: IntRange) {
    fun intersects(otherChromosome: String, otherRange: IntRange): Boolean {
        return chromosome == otherChromosome &&
                this.range.first <= otherRange.last && otherRange.first <= this.range.last
    }
}

/**
 * Selects random regions from the given twoBit file that match gc content and length of sequences
 * from a given fasta file.
 *
 * @param twoBit The twoBit file
 * @param inputFasta The input fasta file we're matching
 * @param outputFasta The fasta file to output our random sequences to
 * @param outputsPerInput the number of matching output sequences to find per input sequence
 * @param chromosomeSizes chromosome size information for given twoBit file
 * @param gcContentTolerance max acceptable distance from gcContent that will still be accepted
 */
fun randomSequences(twoBit: Path,
                    inputFasta: Path,
                    outputFasta: Path,
                    outputsPerInput: Int,
                    chromosomeSizes: Map<String, Int>,
                    gcContentTolerance: Double) {
    // Keep one parser per chromosome, because the "setCurrentSequence" method takes time and caches header data.
    val parsers = mutableMapOf<String, TwoBitParser>()

    val inputSequences = parseFastaSequences(inputFasta)
    val outputSequences = mutableListOf<Sequence>()
    for (inputSequence in inputSequences) {
        val sequenceLength = inputSequence.length
        val inputGCContent = sequenceGCContent(inputSequence)

        var currentOutputsForInput = 0
        while (currentOutputsForInput < outputsPerInput) {
            // Find another sequence to add

            // Get a random chromosome (probability weighted by chromosome sizes)
            val randomChromosome = weightedRandomChromosome(chromosomeSizes)

            // Get a random range with length "sequenceLength" on that chromosome
            val randomStart = (0 .. chromosomeSizes.getValue(randomChromosome) - sequenceLength).random()
            val randomRange = randomStart until randomStart + sequenceLength

            // If we've already added a sequence that intersects with this range, try again
            val intersectsExisting = outputSequences.any { it.intersects(randomChromosome, randomRange) }
            if (intersectsExisting) continue

            val parser = parsers.getOrPut(randomChromosome) {
                val p = TwoBitParser(twoBit.toFile())
                p.setCurrentSequence(randomChromosome)
                p
            }

            // Collect sequence from file
            val parsedSeq = parser.loadFragment(randomStart.toLong(), sequenceLength)
            val sequence = Sequence(parsedSeq, randomChromosome, randomRange)

            val outputGCContent = sequenceGCContent(sequence.seq)
            val acceptableGCContentRange =
                    (inputGCContent - gcContentTolerance) .. (inputGCContent + gcContentTolerance)
            if (acceptableGCContentRange.contains(outputGCContent)) continue
            outputSequences += sequence
            currentOutputsForInput++
        }
    }


    Files.createDirectories(outputFasta.parent)
    Files.newBufferedWriter(outputFasta).use { writer ->
        for ((index, sequence) in outputSequences.withIndex()) {
            writer.write(">${RANDOM_PREFIX}_$index\n")
            writer.write("${sequence.seq}\n")
        }
    }
}

private fun sequenceGCContent(sequence: String) =
        sequence.count { "gc".contains(it, ignoreCase = true) } / sequence.length.toDouble()

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

/**
 * Gets a random chromosome with probability weighted by chromosome length.
 */
private fun weightedRandomChromosome(chromosomeSizes: Map<String, Int>): String {
    val totalSize = chromosomeSizes.values.sum()
    var random = (0 .. totalSize).random()
    var selectedChromosome: String? = null
    for ((chromosome, size) in chromosomeSizes) {
        selectedChromosome = chromosome
        random -= size
        if (random <= 0) break
    }
    return selectedChromosome!!
}