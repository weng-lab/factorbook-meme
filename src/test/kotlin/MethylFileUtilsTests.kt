import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import util.*


class MethylFileUtilsTests {
    @BeforeEach fun setup() = setupTest()
    @AfterEach fun cleanup() = cleanupTest()

    @Test fun `test MethylData#containsValueInRange`() {
        val methylData = MethylData(mapOf("chr1" to listOf(1000, 1100, 2000)))
        assertThat(methylData.containsValueInRange("chr1", 0 .. 100)).isEqualTo(false)
        assertThat(methylData.containsValueInRange("chr1", 1099 .. 1099)).isEqualTo(false)
        assertThat(methylData.containsValueInRange("chr1", 2050 .. 3000)).isEqualTo(false)
        assertThat(methylData.containsValueInRange("chr1", 0 .. 5000)).isEqualTo(true)
        assertThat(methylData.containsValueInRange("chr1", 999 .. 1001)).isEqualTo(true)
        assertThat(methylData.containsValueInRange("chr1", 1001 .. 1001)).isEqualTo(true)
        assertThat(methylData.containsValueInRange("chr1", 1000 .. 1000)).isEqualTo(true)
        assertThat(methylData.containsValueInRange("chr1", 1098 .. 1100)).isEqualTo(true)
    }

    @Test fun `test MethylData#replaceBases`() {
        val methylData = MethylData(mapOf("chr1" to listOf(1000, 1005)))

        var segmentWithReplacements = methylData.replaceBases("ttCGCaccgttg", "chr1", 998 .. 1010)
        assertThat(segmentWithReplacements).isEqualTo("ttMWCacmwttg")
        segmentWithReplacements = methylData.replaceBases("CGCaccg", "chr1", 1000 .. 1006)
        assertThat(segmentWithReplacements).isEqualTo("MWCacmw")
        segmentWithReplacements = methylData.replaceBases("GCacc", "chr1", 1001 .. 1005)
        assertThat(segmentWithReplacements).isEqualTo("WCacm")
        segmentWithReplacements = methylData.replaceBases("Cac", "chr1", 1002 .. 1004)
        assertThat(segmentWithReplacements).isEqualTo("Cac")
    }

    @Test fun `test parseMethylBeds for single methylBed`() {
        val methylData = parseMethylBeds(listOf(METHYL_BED), 50)
        assertThat(methylData.containsValueInRange("chr19", 66015 .. 66025)).isEqualTo(false)
        assertThat(methylData.containsValueInRange("chr19", 70041 .. 70042)).isEqualTo(false)
        assertThat(methylData.containsValueInRange("chr19", 1000 .. 166065)).isEqualTo(true)
        assertThat(methylData.containsValueInRange("chr19", 66055 .. 66065)).isEqualTo(true)
    }

    @Test fun `test parseMethylBeds for multiple methylBeds`() {
        val methylData = parseMethylBeds(listOf(METHYL_BED_2A, METHYL_BED_2B), 50)
        // zero in both beds
        assertThat(methylData.containsValueInRange("chr22", 10520844 .. 10520844)).isEqualTo(false)
        // avg is less than 50
        assertThat(methylData.containsValueInRange("chr22", 10522482 .. 10522482)).isEqualTo(false)
        // avg is greater than 50
        assertThat(methylData.containsValueInRange("chr22", 10522978 .. 10522978)).isEqualTo(true)
        assertThat(methylData.containsValueInRange("chr22", 10522979 .. 10522979)).isEqualTo(true)
    }
}