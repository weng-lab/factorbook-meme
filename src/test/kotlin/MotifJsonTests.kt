import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.Okio
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import step.*
import util.*
import java.nio.file.Path

private val STANDARD_BASES = arrayOf('A', 'C', 'T', 'G')
private val METHYL_BASES = arrayOf('M', 'W')
private val ALL_BASES = STANDARD_BASES + METHYL_BASES

class MotifJsonTests {
    @BeforeEach fun setup() = setupTest()
    @AfterEach fun cleanup() = cleanupTest()

    @Test
    fun `test parseMotifs`() {
        val motifData = parseMotifs(testInputDir.resolve(TOP500_MEME_XML))
        assertThat(motifData.motifs).hasSize(5)
        for (motif in motifData.motifs) {
            assertThat(motif.name.length).isGreaterThan(0)
            assertThat(motif.name.length).isEqualTo(motif.pwm.size)
            for (pwmEntry in motif.pwm) {
                assertThat(pwmEntry).containsKeys(*STANDARD_BASES)
                assertThat(pwmEntry).doesNotContainKeys(*METHYL_BASES)
            }
        }
        assertThat(motifData.letterFrequencies).containsKeys(*STANDARD_BASES)
        assertThat(motifData.letterFrequencies).doesNotContainKeys(*METHYL_BASES)
    }

    @Test
    fun `test parseMotifs for methylated motifs`() {
        val motifData = parseMotifs(testInputDir.resolve(M_TOP500_MEME_XML))
        assertThat(motifData.motifs).hasSize(5)
        for (motif in motifData.motifs) {
            assertThat(motif.name.length).isGreaterThan(0)
            assertThat(motif.name.length).isEqualTo(motif.pwm.size)
            for (pwmEntry in motif.pwm) {
                assertThat(pwmEntry).containsKeys(*ALL_BASES)
            }
        }
        assertThat(motifData.letterFrequencies).containsKeys(*ALL_BASES)
    }

    @Test
    fun `test parseNumSequences`() {
        val numSequences = parseNumSequences(testInputDir.resolve(TOP501_1000_CENTER_FIMO_XML))
        assertThat(numSequences).isEqualTo(500)
    }

    @Test
    fun `test motifOccurrencesCounts`() {
        val memeMotifNames = parseMotifs(testInputDir.resolve(TOP500_MEME_XML)).motifs.map { it.name }
        val occurrencesCounts = motifOccurrencesCounts(testInputDir.resolve(TOP501_1000_CENTER_FIMO_TSV), memeMotifNames)
        val expectedOccurrencesCounts = mapOf(
                "ATABGYCCATTGCTAGTAGGTGCCGGTGCT" to 25,
                "CCTGTCYGGGCATRACAGARGGCTCRCAC" to 24,
                "RGCGCCCYCTRGTGGC" to 437,
                "TTCCCAGRCGCTGGCRTTACCGCTAGACCA" to 23,
                "TTYTYTWTTYTTDTTTTTWKW" to 12
        )
        assertThat(occurrencesCounts).isEqualTo(expectedOccurrencesCounts)
    }

    @Test
    fun `test full motifQuality step`() {
        val outJson = testOutputDir.resolve(MOTIFS_JSON)
        motifJson(
                testInputDir.resolve(TOP500_MEME_XML),
                testInputDir.resolve(FIMO_DIR),
                testInputDir.resolve(TOP501_1000_CENTER_FIMO_DIR),
                testInputDir.resolve(TOP501_1000_SHUFFLED_FIMO_DIR),
                testInputDir.resolve(TOP501_1000_FLANK_FIMO_DIR),
                testInputDir.resolve(CLEANED_PEAKS),
                outJson
        )
        assertThat(outJson).exists()

        val outputMotifData = parseOutJson(outJson)
        assertThat(outputMotifData.motifs).hasSize(5)
        for (outputMotif in outputMotifData.motifs) {
            for (pwmEntry in outputMotif.pwm) {
                for (base in STANDARD_BASES) {
                    assertThat(pwmEntry[base]).isBetween(0.0, 1.0)
                }
            }
            assertThat(outputMotif.eValue).isBetween(0.0, 1.0)
            assertThat(outputMotif.sites).isGreaterThan(0)
            assertThat(outputMotif.lesserPeaksOccurrencesRatio).isBetween(0.0, 1.0)
            assertThat(outputMotif.flankControlData.occurrencesRatio).isBetween(0.0, 1.0)
            assertThat(outputMotif.shuffledControlData.occurrencesRatio).isBetween(0.0, 1.0)
        }
    }

    @Test
    fun `test full motifQuality step on methyl run files`() {
        val outJson = testOutputDir.resolve(M_MOTIFS_JSON)
        motifJson(
                testInputDir.resolve(M_TOP500_MEME_XML),
                testInputDir.resolve(M_FIMO_DIR),
                testInputDir.resolve(M_TOP501_1000_CENTER_FIMO_DIR),
                testInputDir.resolve(M_TOP501_1000_SHUFFLED_FIMO_DIR),
                testInputDir.resolve(M_TOP501_1000_FLANK_FIMO_DIR),
                testInputDir.resolve(M_CLEANED_PEAKS),
                outJson
        )
        assertThat(outJson).exists()

        val outputMotifData = parseOutJson(outJson)
        assertThat(outputMotifData.motifs).hasSize(5)
        for (outputMotif in outputMotifData.motifs) {
            for (pwmEntry in outputMotif.pwm) {
                for (base in ALL_BASES) {
                    assertThat(pwmEntry[base]).isBetween(0.0, 1.0)
                }
            }
            assertThat(outputMotif.eValue).isBetween(0.0, 1.0)
            assertThat(outputMotif.sites).isGreaterThan(0)
            assertThat(outputMotif.lesserPeaksOccurrencesRatio).isBetween(0.0, 1.0)
            assertThat(outputMotif.flankControlData.occurrencesRatio).isBetween(0.0, 1.0)
            assertThat(outputMotif.shuffledControlData.occurrencesRatio).isBetween(0.0, 1.0)
        }
        for (base in ALL_BASES) {
            assertThat(outputMotifData.backgroundFrequencies[base]).isBetween(0.0, 1.0)
        }
    }

    @Test
    fun `compareOccurrenceProportions with 0 occurrences`() {
        val zScore = compareOccurrenceProportions(OccurrenceRatioData(0.0, 0, 100),
                OccurrenceRatioData(0.0, 0, 200))
        assertThat(zScore).isEqualTo(0.0)
    }
}

private fun parseOutJson(outJson: Path): MotifData {
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val adapter = moshi.adapter(MotifData::class.java)
    return adapter.fromJson(Okio.buffer(Okio.source(outJson)))!!
}