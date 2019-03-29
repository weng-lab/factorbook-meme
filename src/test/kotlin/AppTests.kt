import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.*
import util.*

/**
 * Test the complete meme application minus CLI bootstrap.
 * Should be run manually only. Leave disabled so it doesn't get picked up by automated tests.
 * The application takes about 20 minutes to complete.
 */
@Disabled class CompleteAppTests {
    @BeforeEach fun setup() = setupTest()
    @AfterEach fun cleanup() = cleanupTest()

    @Test fun `run complete task`() {
        cmdRunner.runTask(PEAKS, TWO_BIT, CHROM_INFO, 10, testOutputDir)

        assertOutputMatches(SUMMITS)
        assertOutputMatches(TOP500_TRIMMED)
        assertOutputMatches(TOP500_SEQS)
        assertOutputMatches(TOP500_SEQS_CENTER)
        assertOutputMatches(TOP500_SEQS_FLANK)
        assertThat(testOutputDir.resolve(TOP500_MEME_XML)).exists()
        assertThat(testOutputDir.resolve(TOP500_FIMO_TSV)).exists()
        assertOutputMatches(TOP501_1000_TRIMMED)
        assertOutputMatches(TOP501_1000_SEQS)
        assertOutputMatches(TOP501_1000_SEQS_CENTER)
        assertOutputMatches(TOP501_1000_SEQS_FLANK)
        assertThat(testOutputDir.resolve(TOP501_1000_MEME_XML)).exists()
        assertThat(testOutputDir.resolve(TOP501_1000_FIMO_TSV)).exists()
    }
}