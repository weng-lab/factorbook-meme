package util

import java.nio.file.*

class JunkClass

fun exportResourceFile(resourceName: String, to: Path) {
    val resourceStream = JunkClass::class.java.classLoader.getResourceAsStream(resourceName)
    Files.copy(resourceStream, to, StandardCopyOption.REPLACE_EXISTING)
}