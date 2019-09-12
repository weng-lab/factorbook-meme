import org.junit.jupiter.api.*
import step.*
import util.*

class SequencesTests {
    @BeforeEach fun setup() = setupTest()
    @AfterEach fun cleanup() = cleanupTest()

    @Test fun `run trim-peaks step`() {
        trimPeaks(testInputDir.resolve(SUMMITS), testOutputDir.resolve(TOP500_TRIMMED), (0 until 500), TEST_CHR_FILTER)
        assertOutputMatches(TOP500_TRIMMED)
    }

    @Test fun `run peaks-to-fasta step`() {
        cmdRunner.peaksToFasta(testInputDir.resolve(TOP500_TRIMMED), CHR22_TWO_BIT,
            testOutputDir.resolve(TOP500_SEQS))
        assertOutputMatches(TOP500_SEQS)
    }

    @Test fun `run fasta-center step`() {
        cmdRunner.fastaCenter(testInputDir.resolve(TOP501_1000_SEQS), 100,
            testOutputDir.resolve(TOP501_1000_SEQS_CENTER), testOutputDir.resolve(TOP501_1000_SEQS_FLANK))
        assertOutputMatches(TOP501_1000_SEQS_CENTER)
        assertOutputMatches(TOP501_1000_SEQS_FLANK)
    }
}