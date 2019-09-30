package util

import mu.KotlinLogging
import java.nio.file.*


private val log = KotlinLogging.logger {}

class JunkClass

fun exportResourceFile(resourceName: String, to: Path) {
    val resourceStream = JunkClass::class.java.classLoader.getResourceAsStream(resourceName)
    Files.copy(resourceStream, to, StandardCopyOption.REPLACE_EXISTING)
}

/**
 * Utility function for easily retrying an arbitrary block of code the given number of times before failing.
 */
fun <T> retry(name: String, numOfRetries: Int, block: () -> T): T {
    var throwable: Throwable? = null
    (1..numOfRetries).forEach { attempt ->
        try {
            return block()
        } catch (e: Throwable) {
            throwable = e
            log.error(e) { "Failed $name attempt $attempt / $numOfRetries" }
        }
    }
    throw throwable!!
}