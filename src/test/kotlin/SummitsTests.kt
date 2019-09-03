import org.junit.jupiter.api.*
import step.*
import util.*

class SummitsTests {
    @BeforeEach fun setup() = setupTest()
    @AfterEach fun cleanup() = cleanupTest()

    @Test fun `run summits step`() {
        val chromSizes = parseChromSizes(CHR22_CHROM_INFO)
        summits(PEAKS, chromSizes, 150, testOutputDir.resolve(SUMMITS), 10)

        assertOutputMatches(SUMMITS)
    }
}