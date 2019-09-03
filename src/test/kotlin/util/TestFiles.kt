package util

import org.assertj.core.api.Assertions.*
import java.nio.file.*

fun getResourcePath(relativePath: String): Path {
    val url = TestCmdRunner::class.java.classLoader.getResource(relativePath)
    return Paths.get(url.toURI())
}

fun assertOutputMatches(filename: String) =
    assertThat(testOutputDir.resolve(filename)).hasSameContentAs(testOutputResourcesDir.resolve(filename))!!

// Resource Directories
val testInputResourcesDir = getResourcePath("test-input-files")
val testOutputResourcesDir = getResourcePath("test-output-files")

// Test Working Directories
val testDir = Paths.get("/tmp/motif-test")!!
val testInputDir = testDir.resolve("input")!!
val testOutputDir = testDir.resolve("output")!!

// Input Files
val PEAKS= testInputDir.resolve("ENCFF165UME.bed")!!
val CHR22_TWO_BIT= testInputDir.resolve("hg38.chr22.2bit")!!
val CHR22_CHROM_INFO = testInputDir.resolve("hg38.chr22.chrom.sizes")!!
val CHR19_TWO_BIT= testInputDir.resolve("hg38.chr19.2bit")!!
val CHR19_CHROM_INFO = testInputDir.resolve("hg38.chr19.chrom.sizes")!!

// Output File Names
const val SUMMITS = "ENCFF165UME.summits.window150.narrowPeak"
const val TOP500_TRIMMED = "ENCFF165UME.top500.narrowPeak.trimmed"
const val TOP500_SEQS = "ENCFF165UME.top500.seqs"
const val TOP500_SEQS_CENTER = "ENCFF165UME.top500.seqs.center"
const val TOP500_MEME_DIR = "ENCFF165UME.top500.center.meme"
const val TOP500_MEME_TXT = "$TOP500_MEME_DIR/meme.txt"
const val TOP500_MEME_XML = "$TOP500_MEME_DIR/meme.xml"
const val TOP501_1000_TRIMMED = "ENCFF165UME.top501-1000.narrowPeak.trimmed"
const val TOP501_1000_SEQS = "ENCFF165UME.top501-1000.seqs"
const val TOP501_1000_SEQS_CENTER = "ENCFF165UME.top501-1000.seqs.center"
const val TOP501_1000_SEQS_FLANK = "ENCFF165UME.top501-1000.seqs.flank"
const val TOP501_1000_CENTER_FIMO_DIR = "ENCFF165UME.top501-1000.center.fimo"
const val TOP501_1000_CENTER_FIMO_TSV = "$TOP501_1000_CENTER_FIMO_DIR/fimo.tsv"
const val TOP501_1000_CENTER_FIMO_XML = "$TOP501_1000_CENTER_FIMO_DIR/fimo.xml"
const val TOP501_1000_FLANK_FIMO_DIR = "ENCFF165UME.top501-1000.flank.fimo"
const val TOP501_1000_FLANK_FIMO_TSV = "$TOP501_1000_FLANK_FIMO_DIR/fimo.tsv"
const val TOP501_1000_SHUFFLED_SEQS = "ENCFF165UME.top501-1000.shuffled.seqs"
const val TOP501_1000_SHUFFLED_FIMO_DIR = "ENCFF165UME.top501-1000.shuffled.fimo"
const val TOP501_1000_SHUFFLED_FIMO_TSV = "$TOP501_1000_SHUFFLED_FIMO_DIR/fimo.tsv"