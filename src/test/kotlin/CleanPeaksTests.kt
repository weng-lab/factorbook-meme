import org.junit.jupiter.api.*
import step.cleanPeaks
import util.*

class CleanPeaksTests {
    @BeforeEach fun setup() = setupTest()
    @AfterEach fun cleanup() = cleanupTest()

    @Test fun `Test cleanPeaks without methyl bed`() {
        cleanPeaks(PEAKS, TEST_CHR_FILTER, null, testOutputDir.resolve(CLEANED_PEAKS))
        assertOutputMatches(CLEANED_PEAKS)
    }

    @Test fun `Test cleanPeaks with methyl bed`() {
        val methylData = parseMethylBeds(listOf(METHYL_BED), 50)
        cleanPeaks(M_PEAKS, null, methylData, testOutputDir.resolve(M_CLEANED_PEAKS))
        assertOutputMatches(M_CLEANED_PEAKS)
    }

    @Test fun `Test cleanPeaks with multiple methyl beds`() {
        val methylData = parseMethylBeds(listOf(METHYL_BED_2A, METHYL_BED_2B), 50)
        cleanPeaks(M2_PEAKS, null, methylData, testOutputDir.resolve(M2_CLEANED_PEAKS))
        assertOutputMatches(M2_CLEANED_PEAKS)
    }

}