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

        var memeXml =  MOTIF_MEME_XML

        cmdRunner.tomtom(memeXml.fileName.toString().split(".").first(),testOutputDir,memeXml, listOf(COMPARISON_DB1,COMPARISON_DB2,COMPARISON_DB3) ,0.5)
        assertThat(testOutputDir.resolve(TOMTOM_TSV)).exists()
        assertThat(testOutputDir.resolve(TOMTOM_XML)).exists()
        

    }



}