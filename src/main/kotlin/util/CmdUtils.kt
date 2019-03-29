package util

import mu.KotlinLogging

private val log = KotlinLogging.logger {}

interface CmdRunner {
    fun run(cmd: String)
}

class DefaultCmdRunner : CmdRunner {
    override fun run(cmd: String) = exec(*cmd.split(" ").toTypedArray())
}

fun exec(vararg cmds: String) {
    log.info { "Executing command: ${cmds.toList()}" }
    val exitCode = ProcessBuilder(*cmds)
        .inheritIO()
        .start()
        .waitFor()
    if (exitCode != 0) {
        throw Exception("command failed with exit code $exitCode")
    }
}