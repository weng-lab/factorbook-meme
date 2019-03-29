import org.assertj.core.api.Assertions
import org.junit.jupiter.api.*
import step.*
import util.*

class MemeSuiteTests {
    @BeforeEach fun setup() = setupTest()
    @AfterEach fun cleanup() = cleanupTest()

    /**
     * Test the meme step.
     * Should be run manually only. Leave disabled so it doesn't get picked up by automated tests.
     * The MEME step takes about 20 minutes to complete.
     */
    @Disabled @Test fun `run meme step`() {
        cmdRunner.meme(testInputDir.resolve(TOP500_SEQS_CENTER), testOutputDir.resolve(TOP500_MEME_DIR))
        Assertions.assertThat(testOutputDir.resolve(TOP500_MEME_TXT)).exists()
        Assertions.assertThat(testOutputDir.resolve(TOP500_MEME_XML)).exists()
    }

    @Test fun `run fimo step`() {
        cmdRunner.fimo(testInputDir.resolve(TOP500_MEME_TXT), testInputDir.resolve(TOP500_SEQS_CENTER),
            testOutputDir.resolve(TOP500_FIMO_DIR))
        Assertions.assertThat(testOutputDir.resolve(TOP500_FIMO_TSV)).exists()
    }

}