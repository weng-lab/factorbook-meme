package step

import util.CmdRunner
import java.nio.file.Files
import java.nio.file.Path

/**
 * Calls tomtotm on a meme xml File.
 *
 * @param memeXml input meme Xml file.
 * @param outputDir path to write output meme files.
 * @param threshold threshold.
 * @param comparisonDatabases comparison databases
 */
fun CmdRunner.tomtom(outPrefix: String,  outputDir: Path,memeXml:Path,comparisonDatabases: List<Path>?,threshold: Double?) {
    Files.createDirectories(outputDir.parent)
    var memePrefix =  outPrefix
    this.run(" tomtom -thresh ${threshold}  -oc $outputDir  ${memeXml} \\\n" +
            "            ${comparisonDatabases!!.map { it.toString() }.toTypedArray().joinToString(" ") { it }}")    
    this.run("cp $outputDir/tomtom.tsv $outputDir/$memePrefix.tomtom.tsv")
    this.run("cp $outputDir/tomtom.xml $outputDir/$memePrefix.tomtom.xml")
}
