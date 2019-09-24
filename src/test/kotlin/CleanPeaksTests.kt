import org.junit.jupiter.api.*
import step.cleanPeaks
import util.*

class CleanPeaksTests {
    @BeforeEach fun setup() = setupTest()
    @AfterEach fun cleanup() = cleanupTest()

    @Test fun `Test cleanPeaks without methyl bed`() {
        cleanPeaks(PEAKS, TEST_CHR_FILTER, null, null,
                testOutputDir.resolve(CLEANED_PEAKS))
        assertOutputMatches(CLEANED_PEAKS)
    }

    @Test fun `Test cleanPeaks with methyl bed`() {
        cleanPeaks(M_PEAKS, null, METHYL_BED, 50,
                testOutputDir.resolve(M_CLEANED_PEAKS))
        assertOutputMatches(M_CLEANED_PEAKS)
    }

}