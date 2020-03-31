package step

import util.*
import java.nio.file.*

const val DEFAULT_MEME_OPTIONS = "-dna -mod zoops -nmotifs 5 -minw 6 -maxw 30 -revcomp"
const val METHYL_ALPHABET_FILENAME = "methyl_alphabet.txt"

/**
 * Calls meme on a FASTA File.
 *
 * @param fastaIn input FASTA file.
 * @param outputDir path to write output meme files.
 * @param memeOptions command line arguments to run meme with.
 */
fun CmdRunner.meme(fastaIn: Path, outputDir: Path, useMotifAlphabet: Boolean = false, memeOptions: String = DEFAULT_MEME_OPTIONS) {
    Files.createDirectories(outputDir.parent)
    var alphabetOptions = ""
    if (useMotifAlphabet) {
        val alphabetFile = outputDir.parent.resolve(METHYL_ALPHABET_FILENAME)
        exportResourceFile(METHYL_ALPHABET_FILENAME, alphabetFile)
        alphabetOptions = "-alph $alphabetFile "
    }
    this.run("meme -nostatus -oc $outputDir $memeOptions $alphabetOptions$fastaIn")
}

/**
 * Calls FIMO on a MEME output file and FASTA file
 *
 * @param memeIn input MEME txt file.
 * @param fastaIn input FASTA file.
 * @param output path to write output fimo files.
 */
fun CmdRunner.fimo(memeIn: Path, fastaIn: Path, outputDir: Path) {
    Files.createDirectories(outputDir.parent)
    this.run("fimo -oc $outputDir $memeIn $fastaIn")
}
