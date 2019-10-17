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

fun <T> List<T>.rangeBinarySearch(range: IntRange, selector: (value: T) -> Int = { it as Int }): List<T> {
    val rangeFirstSearchResult = this.binarySearchBy(range.first - 1, selector = selector)
    var subListStart = rangeFirstSearchResult
    if (rangeFirstSearchResult < 0) {
        // If not found, binarySearch returns the negative of the "insertion point" or where the value
        // would be inserted to maintain sort order
        subListStart = -rangeFirstSearchResult - 1
        if (subListStart >= this.size || selector(this[subListStart]) > range.last) return listOf()
    }

    val rangeLastSearchResult = this.binarySearchBy(range.last, selector = selector)
    val subListEnd =
            if (rangeLastSearchResult < 0) -rangeLastSearchResult - 1
            else rangeLastSearchResult + 1
    return this.subList(subListStart, subListEnd)
}
