package step

import util.CmdRunner
import java.nio.file.*

const val DEFAULT_MEME_OPTIONS = "-dna -mod zoops -nmotifs 5 -minw 6 -maxw 30 -revcomp"

/**
 * Calls meme on a FASTA File.
 *
 * @param fastaIn input FASTA file.
 * @param outputDir path to write output meme files.
 * @param memeOptions command line arguments to run meme with.
 */
fun CmdRunner.meme(fastaIn: Path, outputDir: Path, memeOptions: String = DEFAULT_MEME_OPTIONS) {
    Files.createDirectories(outputDir.parent)
    this.run("meme -nostatus -oc $outputDir $memeOptions $fastaIn")
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