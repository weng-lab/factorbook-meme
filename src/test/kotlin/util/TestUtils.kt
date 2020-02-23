package util

import java.nio.file.*

val cmdRunner = TestCmdRunner()

val TEST_CHR_FILTER = setOf("chrJunk")

class TestCmdRunner : CmdRunner {
    override fun run(cmd: String) = exec("docker", "exec", "motif-meme-base", "sh", "-c", cmd)
}

fun copyDirectory(fromDir: Path, toDir: Path) {
    Files.walk(fromDir).forEach { fromFile ->
        if (Files.isRegularFile(fromFile)) {
            val toFile = toDir.resolve(fromDir.relativize(fromFile))
            Files.createDirectories(toFile.parent)
            Files.copy(fromFile, toFile)
        }
    }
}

fun setupTest() {
    cleanupTest()
    // Copy all resource files from "test-input-files" and "test-output-files" dirs into
    // docker mounted working /tmp dir
    copyDirectory(testInputResourcesDir, testInputDir)
    copyDirectory(testOutputResourcesDir, testInputDir)
}

fun cleanupTest() {
    cmdRunner.run("rm -rf ${testInputDir.toAbsolutePath()}/* ${testOutputDir.toAbsolutePath()}/*")
}