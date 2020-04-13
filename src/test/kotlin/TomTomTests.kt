import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.*
import step.*
import util.*

class TomTomSuiteTests {
    @BeforeEach fun setup() = setupTest()
    @AfterEach fun cleanup() = cleanupTest()

    /**
     * Test the tomtom step.           
     */
     @Test fun `run tomtom`() {
        var comparisonDb1 = testInputDir.resolve("motifs_test_HOCOMOCO.human.txt")
        var comparisonDb2 = testInputDir.resolve("motifs_test_HOCOMOCO.mouse.txt")
        var comparisonDb3 = testInputDir.resolve("motifs_test_JASPER.txt")

        var memeXml =  testInputDir.resolve("motifs_outputs_ENCFF002CHV.meme.xml")

        cmdRunner.tomtom(memeXml.fileName.toString().split(".").first(),testOutputDir,memeXml, listOf(comparisonDb1,comparisonDb2,comparisonDb3) ,0.5)
        assertThat(testOutputDir.resolve("motifs_outputs_ENCFF002CHV.tomtom.xml")).exists()

    }



}