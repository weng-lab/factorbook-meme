import org.junit.jupiter.api.*
import org.knowm.xchart.*
import org.knowm.xchart.style.lines.SeriesLines
import org.knowm.xchart.style.markers.SeriesMarkers
import step.peakCentrality
import util.*

@Disabled
class Plot {
    @BeforeEach fun setup() = setupTest()
    @AfterEach fun cleanup() {
        cleanupTest()
        Thread.sleep(Long.MAX_VALUE)
    }

    @Test
    fun `Plot peak centrality standard sample`() {
        val peakCentrality = peakCentrality(testInputDir.resolve(FIMO_TSV), testInputDir.resolve(CLEANED_PEAKS))
        val charts = peakCentrality.map { (motif, pcData) -> peakCentralityChart(motif, pcData) }
        SwingWrapper(charts).displayChartMatrix()
    }

    @Test
    fun `Plot peak centrality for methyl sample`() {
        val peakCentrality = peakCentrality(testInputDir.resolve(M_FIMO_TSV), testInputDir.resolve(M_CLEANED_PEAKS))
        val charts = peakCentrality.map { (motif, pcData) -> peakCentralityChart(motif, pcData) }
        SwingWrapper(charts).displayChartMatrix()
    }

}

private fun peakCentralityChart(name: String, data: Map<Int, Double>): XYChart {
    val chart = XYChartBuilder()
            .title("Peak Centrality for $name")
            .width(800)
            .height(600)
            .xAxisTitle("Distance from Summit (in BP)")
            .build()

    val points = data.entries
    val series = chart.addSeries("Data", points.map { it.key }, points.map { it.value })
    series.xySeriesRenderStyle = XYSeries.XYSeriesRenderStyle.StepArea
    series.marker = SeriesMarkers.NONE
    series.lineStyle = SeriesLines.NONE
    return chart
}