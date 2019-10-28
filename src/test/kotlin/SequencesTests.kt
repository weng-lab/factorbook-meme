import org.junit.jupiter.api.*
import step.*
import util.*

class SequencesTests {
    @BeforeEach fun setup() = setupTest()
    @AfterEach fun cleanup() = cleanupTest()

    @Test fun `run peaks-to-fasta step`() {
        peaksToFasta(testInputDir.resolve(SUMMITS), CHR19_TWO_BIT, testOutputDir.resolve(TOP500_SEQS),
                lineRange = 0 until 500)
        assertOutputMatches(TOP500_SEQS)
    }

    @Test fun `run peaks-to-fasta step with methylation state replacements`() {
        val methylData = parseMethylBeds(listOf(METHYL_BED), 50)
        peaksToFasta(testInputDir.resolve(M_SUMMITS), CHR19_TWO_BIT, testOutputDir.resolve(M_TOP500_SEQS),
                methylData, 0 until 500)
        assertOutputMatches(M_TOP500_SEQS)
    }

    @Test fun `run fasta-center step`() {
        cmdRunner.fastaCenter(testInputDir.resolve(TOP501_1000_SEQS), 100,
            testOutputDir.resolve(TOP501_1000_SEQS_CENTER), testOutputDir.resolve(TOP501_1000_SEQS_FLANK))
        assertOutputMatches(TOP501_1000_SEQS_CENTER)
        assertOutputMatches(TOP501_1000_SEQS_FLANK)
    }
}