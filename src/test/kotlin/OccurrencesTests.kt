import org.junit.jupiter.api.*
import step.occurrencesTsv
import util.*

class OccurrencesTests {
    @BeforeEach fun setup() = setupTest()
    @AfterEach fun cleanup() = cleanupTest()

    @Test
    fun `test occurrencesTsv`() {
        occurrencesTsv(testInputDir.resolve(FIMO_TSV), testInputDir.resolve(CLEANED_PEAKS),
                testOutputDir.resolve(OCCURRENCES_TSV))
        assertOutputMatches(OCCURRENCES_TSV)
    }

}