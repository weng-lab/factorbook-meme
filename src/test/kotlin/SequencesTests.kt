import org.junit.jupiter.api.*
import step.*
import util.*

class SequencesTests {
    @BeforeEach fun setup() = setupTest()
    @AfterEach fun cleanup() = cleanupTest()

    @Test fun `run trim-peaks step`() {
        trimPeaks(testInputDir.resolve(SUMMITS), (0..500), testOutputDir.resolve(TOP500_TRIMMED))
        assertOutputMatches(TOP500_TRIMMED)
    }

    @Test fun `run peaks-to-fasta step`() {
        cmdRunner.peaksToFasta(testInputDir.resolve(TOP500_TRIMMED), TWO_BIT,
            testOutputDir.resolve(TOP500_SEQS))
        assertOutputMatches(TOP500_SEQS)
    }

    @Test fun `run fasta-center step`() {
        cmdRunner.fastaCenter(testInputDir.resolve(TOP500_SEQS), 100,
            testOutputDir.resolve(TOP500_SEQS_FLANK), testOutputDir.resolve(TOP500_SEQS_CENTER))
        assertOutputMatches(TOP500_SEQS_CENTER)
        assertOutputMatches(TOP500_SEQS_FLANK)
    }
}