import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.*
import step.*
import util.*

/**
 * Test the complete meme application minus CLI bootstrap.
 * Should be run manually only. Leave disabled so it doesn't get picked up by automated tests.
 * The application takes about 20 minutes to complete.
 */
@Disabled class CompleteAppTests {
    @BeforeEach fun setup() = setupTest()
    //@AfterEach fun cleanup() = cleanupTest()

    @Test fun `run complete task`() {
        cmdRunner.runTask(PEAKS, CHR22_TWO_BIT, CHR22_CHROM_INFO, 10, testOutputDir, TEST_CHR_FILTER)

        assertOutputMatches(CLEANED_PEAKS)
        assertOutputMatches(SUMMITS)
        assertOutputMatches(BASE_SEQS)
        assertOutputMatches(OCCURRENCES_TSV)
        assertOutputMatches(TOP500_SEQS)
        assertOutputMatches(TOP500_SEQS_CENTER)
        assertThat(testOutputDir.resolve(TOP500_MEME_XML)).exists()
        assertOutputMatches(TOP501_1000_SEQS)
        assertOutputMatches(TOP501_1000_SEQS_CENTER)
        assertOutputMatches(TOP501_1000_SEQS_FLANK)
        assertThat(testOutputDir.resolve(TOP501_1000_CENTER_FIMO_TSV)).exists()
        assertThat(testOutputDir.resolve(TOP501_1000_FLANK_FIMO_TSV)).exists()
        assertThat(testOutputDir.resolve(TOP501_1000_SHUFFLED_SEQS)).exists()
        assertThat(testOutputDir.resolve(TOP501_1000_SHUFFLED_FIMO_TSV)).exists()
        assertThat(testOutputDir.resolve(MOTIFS_JSON)).exists()
    }

    @Test fun `run complete task for methylated peaks`() {
        cmdRunner.runTask(M_PEAKS, CHR19_TWO_BIT, CHR19_CHROM_INFO, 0, testOutputDir, null,
                METHYL_BED, 50)

        assertOutputMatches(M_CLEANED_PEAKS)
        assertOutputMatches(M_SUMMITS)
        assertOutputMatches(M_BASE_SEQS)
        assertOutputMatches(M_OCCURRENCES_TSV)
        assertOutputMatches(M_TOP500_SEQS)
        assertOutputMatches(M_TOP500_SEQS_CENTER)
        assertThat(testOutputDir.resolve(M_TOP500_MEME_XML)).exists()
        assertOutputMatches(M_TOP501_1000_SEQS)
        assertOutputMatches(M_TOP501_1000_SEQS_CENTER)
        assertOutputMatches(M_TOP501_1000_SEQS_FLANK)
        assertThat(testOutputDir.resolve(M_TOP501_1000_CENTER_FIMO_TSV)).exists()
        assertThat(testOutputDir.resolve(M_TOP501_1000_FLANK_FIMO_TSV)).exists()
        assertThat(testOutputDir.resolve(M_TOP501_1000_SHUFFLED_SEQS)).exists()
        assertThat(testOutputDir.resolve(M_TOP501_1000_SHUFFLED_FIMO_TSV)).exists()
        assertThat(testOutputDir.resolve(M_MOTIFS_JSON)).exists()
    }

    @Test fun `run post-meme steps for methylated peaks`() {
        val chr19Sizes = parseChromSizes(CHR19_CHROM_INFO)
        cmdRunner.runQualitySteps(M_PREFIX, testInputDir.resolve(M_SUMMITS), testInputDir.resolve(M_MEME_DIR),
                testInputDir.resolve(M_TOP500_SEQS_CENTER), CHR19_TWO_BIT, testOutputDir, chr19Sizes,
                METHYL_BED, 50)
    }
}