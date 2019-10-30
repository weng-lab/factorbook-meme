import mu.KotlinLogging
import org.assertj.core.api.Assertions.*
import org.biojava.nbio.genome.parsers.twobit.TwoBitParser
import org.junit.jupiter.api.*
import step.*
import util.*

private val log = KotlinLogging.logger {}

/**
 * Test the complete meme application minus CLI bootstrap.
 * Should be run manually only. Leave disabled so it doesn't get picked up by automated tests.
 * The application takes about 20 minutes to complete.
 */
@Disabled class CompleteAppTests {
    @BeforeEach fun setup() = setupTest()
    //@AfterEach fun cleanup() = cleanupTest()

    @Test fun `run complete task`() {
        cmdRunner.runTask(PEAKS, CHR19_TWO_BIT, CHR19_CHROM_INFO, 10, testOutputDir, TEST_CHR_FILTER,
                50, 10)

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

    @Test fun `run post-meme steps for plain peaks sample`() {
        cmdRunner.runPostMemeSteps(PREFIX, testInputDir.resolve(SUMMITS), testInputDir.resolve(MEME_DIR),
                testInputDir.resolve(CLEANED_PEAKS), testInputDir.resolve(TOP500_SEQS_CENTER), CHR19_TWO_BIT,
                testOutputDir, parseChromSizes(CHR19_CHROM_INFO), 50, 10)
    }

    @Test fun `run complete task for methylated peaks`() {
        cmdRunner.runTask(M_PEAKS, CHR19_TWO_BIT, CHR19_CHROM_INFO, 0, testOutputDir, null,
                100, 10, listOf(METHYL_BED), 50)

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
        val methylData = parseMethylBeds(listOf(METHYL_BED), 50)
        cmdRunner.runPostMemeSteps(M_PREFIX, testInputDir.resolve(M_SUMMITS), testInputDir.resolve(M_MEME_DIR),
                testInputDir.resolve(M_CLEANED_PEAKS), testInputDir.resolve(M_TOP500_SEQS_CENTER), CHR19_TWO_BIT,
                testOutputDir, chr19Sizes, 100, 10, methylData)
    }

    @Test fun `run complete task for methylated peaks with 2 methyl beds`() {
        cmdRunner.runTask(M2_PEAKS, CHR22_TWO_BIT, CHR22_CHROM_INFO, 0, testOutputDir, null,
                100, 10, listOf(METHYL_BED_2A, METHYL_BED_2B), 50)

        assertOutputMatches(M2_CLEANED_PEAKS)
        assertOutputMatches(M2_SUMMITS)
        assertOutputMatches(M2_BASE_SEQS)
        assertOutputMatches(M2_OCCURRENCES_TSV)
        assertOutputMatches(M2_TOP500_SEQS)
        assertOutputMatches(M2_TOP500_SEQS_CENTER)
        assertThat(testOutputDir.resolve(M2_TOP500_MEME_XML)).exists()
        assertOutputMatches(M2_TOP501_1000_SEQS)
        assertOutputMatches(M2_TOP501_1000_SEQS_CENTER)
        assertOutputMatches(M2_TOP501_1000_SEQS_FLANK)
        assertThat(testOutputDir.resolve(M2_TOP501_1000_CENTER_FIMO_TSV)).exists()
        assertThat(testOutputDir.resolve(M2_TOP501_1000_FLANK_FIMO_TSV)).exists()
        assertThat(testOutputDir.resolve(M2_TOP501_1000_SHUFFLED_SEQS)).exists()
        assertThat(testOutputDir.resolve(M2_TOP501_1000_SHUFFLED_FIMO_TSV)).exists()
        assertThat(testOutputDir.resolve(M2_MOTIFS_JSON)).exists()
    }

    @Test fun `run post-meme steps for methylated peaks with 2 methyl beds`() {
        val chr22Sizes = parseChromSizes(CHR22_CHROM_INFO)
        val methylData = parseMethylBeds(listOf(METHYL_BED_2A, METHYL_BED_2B), 50)
        cmdRunner.runPostMemeSteps(M2_PREFIX, testInputDir.resolve(M2_SUMMITS), testInputDir.resolve(M2_MEME_DIR),
                testInputDir.resolve(M2_CLEANED_PEAKS), testInputDir.resolve(M2_TOP500_SEQS_CENTER), CHR22_TWO_BIT,
                testOutputDir, chr22Sizes, 100, 10, methylData)
    }

}