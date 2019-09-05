import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.Okio
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import step.*
import util.*
import java.nio.file.Files

class MotifQualityTests {
    @BeforeEach fun setup() = setupTest()
    //@AfterEach fun cleanup() = cleanupTest()

    @Test
    fun `test parseMotifs`() {
        val motifs = parseMotifs(testInputDir.resolve(TOP500_MEME_XML))
        assertThat(motifs).hasSize(5)
        for (motif in motifs) {
            assertThat(motif.name.length).isGreaterThan(0)
            assertThat(motif.name.length).isEqualTo(motif.pwm.size)
            for (pwmEntry in motif.pwm) {
                assertThat(pwmEntry).containsKey('A')
                assertThat(pwmEntry).containsKey('C')
                assertThat(pwmEntry).containsKey('T')
                assertThat(pwmEntry).containsKey('G')
            }
        }
    }

    @Test
    fun `test parseNumSequences`() {
        val numSequences = parseNumSequences(testInputDir.resolve(TOP501_1000_CENTER_FIMO_XML))
        assertThat(numSequences).isEqualTo(369)
    }

    @Test
    fun `test motifOccurrencesCounts`() {
        val occurrencesCounts = motifOccurrencesCounts(testInputDir.resolve(TOP501_1000_CENTER_FIMO_TSV))
        val expectedOccurrencesCounts = mapOf(
                "AGSCCDGGSCCTGGGAGRSAGGRWGVA" to 42,
                "RSYGCCCCCTRSTGG" to 279,
                "SCCBSVKCCCCCGCSCCYKCCCVSCSSCS" to 144,
                "CTATGWCMCCWCAGCTYCTATCNCTKTATG" to 7,
                "TTTHYTTKYWTKTTYWBTTTTTTWWTHWK" to 6
        )
        assertThat(occurrencesCounts).isEqualTo(expectedOccurrencesCounts)
    }

    @Test
    fun `test full motifQuality step`() {
        val outJson = testOutputDir.resolve("motifs.json")
        motifQuality(
                testInputDir.resolve(TOP500_MEME_XML),
                testInputDir.resolve(TOP501_1000_CENTER_FIMO_DIR),
                testInputDir.resolve(TOP501_1000_SHUFFLED_FIMO_DIR),
                testInputDir.resolve(TOP501_1000_FLANK_FIMO_DIR),
                outJson
        )
        assertThat(outJson).exists()

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter<List<OutputMotif>>(
                Types.newParameterizedType(List::class.java, OutputMotif::class.java))
        val outputMotifs = adapter.fromJson(Okio.buffer(Okio.source(outJson)))!!
        assertThat(outputMotifs).hasSize(5)
        for (outputMotif in outputMotifs) {
            for (pwmEntry in outputMotif.pwm) {
                assertThat(pwmEntry['A']).isBetween(0.0, 1.0)
                assertThat(pwmEntry['C']).isBetween(0.0, 1.0)
                assertThat(pwmEntry['T']).isBetween(0.0, 1.0)
                assertThat(pwmEntry['G']).isBetween(0.0, 1.0)
            }
            assertThat(outputMotif.occurrencesRatio).isBetween(0.0, 1.0)
            assertThat(outputMotif.flankControlData.occurrencesRatio).isBetween(0.0, 1.0)
            assertThat(outputMotif.shuffledControlData.occurrencesRatio).isBetween(0.0, 1.0)
        }
    }
}