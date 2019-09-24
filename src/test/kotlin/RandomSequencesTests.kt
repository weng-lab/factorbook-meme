import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.*
import step.*
import util.*
import java.nio.file.Files

class RandomSequencesTests {
    @BeforeEach fun setup() = setupTest()
    @AfterEach fun cleanup() = cleanupTest()

    @Test
    fun `run randomSequences`() {
        val chromSizes = parseChromSizes(CHR19_CHROM_INFO)
        val inputSequences = testInputDir.resolve(TOP501_1000_SEQS_CENTER)
        val outputFasta = testOutputDir.resolve(TOP501_1000_SHUFFLED_SEQS)
        val outputsPerInput = 2
        randomSequences(CHR19_TWO_BIT, inputSequences, outputFasta, outputsPerInput, chromSizes,0.05)
        assertThat(outputFasta).exists()

        val outputLineCount = Files.newBufferedReader(outputFasta).lines().count().toInt()
        // There aren't actually 500 peaks in 501-1000. Because our input bed is so small there are actually only 369.
        assertThat(outputLineCount).isEqualTo(369 * outputsPerInput * 2)
    }

    @Test
    fun `run randomeSequences using input sequences with methyl states and methyl bed`() {
        val chromSizes = parseChromSizes(CHR19_CHROM_INFO)
        val inputSequences = testInputDir.resolve(M_TOP501_1000_SEQS_CENTER)
        val outputFasta = testOutputDir.resolve(M_TOP501_1000_SHUFFLED_SEQS)
        randomSequences(CHR19_TWO_BIT, inputSequences, outputFasta, 2, chromSizes,0.05,
                METHYL_BED, 50)
        assertThat(outputFasta).exists()

        val outputLineCount = Files.newBufferedReader(outputFasta).lines().count().toInt()
        assertThat(outputLineCount).isEqualTo(500 * 2 * 2)
    }

}