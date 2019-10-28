import org.junit.jupiter.api.*
import step.*
import util.*

class SummitsTests {
    @BeforeEach fun setup() = setupTest()
    @AfterEach fun cleanup() = cleanupTest()

    @Test fun `run summits step`() {
        val chromSizes = parseChromSizes(CHR19_CHROM_INFO)
        summits(testInputDir.resolve(CLEANED_PEAKS), chromSizes, 150,
                testOutputDir.resolve(SUMMITS), 10, TEST_CHR_FILTER)

        assertOutputMatches(SUMMITS)
    }

    @Test fun `run summits step on cleaned methyl file without offset`() {
        val chromSizes = parseChromSizes(CHR19_CHROM_INFO)
        summits(testInputDir.resolve(M_CLEANED_PEAKS), chromSizes, 150,
                testOutputDir.resolve(M_SUMMITS), null, null)

        assertOutputMatches(M_SUMMITS)
    }

}