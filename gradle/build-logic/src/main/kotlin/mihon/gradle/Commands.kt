package mihon.gradle

import org.gradle.api.Project
import kotlin.time.Clock
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant

// Git metadata is preferred, but local/source-archive builds must remain buildable when
// Git is unavailable or refuses the workspace (for example because of safe.directory rules).
fun Project.getLatestCommitCount(): String {
    return execOrNull("git rev-list --count HEAD") ?: "1"
}

fun Project.getLatestCommitSha(): String {
    return execOrNull("git rev-parse --short HEAD") ?: "local"
}

/**
 * @param useLatestCommitTime If `true`, the build time is based on the timestamp of the last Git commit;
 *                          otherwise, the current time is used. Both are in UTC.
 * @return An ISO 8601 formatted string representing the build time.
 */
fun Project.getBuildTime(useLatestCommitTime: Boolean): String {
    val commitEpoch = if (useLatestCommitTime) {
        execOrNull("git log -1 --format=%ct")?.toLongOrNull()
    } else {
        null
    }
    return commitEpoch?.let { Instant.fromEpochSeconds(it).toString() } ?: currentBuildTime()
}

fun Project.exec(command: String): String {
    return providers.exec {
        commandLine = command.split(" ")
    }
        .standardOutput
        .asText
        .get()
        .trim()
}

private fun Project.execOrNull(command: String): String? {
    return runCatching { exec(command) }
        .getOrNull()
        ?.takeIf { it.isNotEmpty() }
}

private fun currentBuildTime(): String {
    val now = Clock.System.now()
    return (now - now.nanosecondsOfSecond.nanoseconds).toString()
}
