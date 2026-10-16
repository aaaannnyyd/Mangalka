package tachiyomi.domain.release.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.release.model.Release
import tachiyomi.domain.release.service.ReleaseService

class GetApplicationReleaseTest {

    private lateinit var getApplicationRelease: GetApplicationRelease
    private lateinit var releaseService: ReleaseService

    @BeforeEach
    fun beforeEach() {
        releaseService = mockk()

        getApplicationRelease = GetApplicationRelease(releaseService)
    }

    @Test
    fun `When has update but is nightly expect new update`() = runTest {
        val release = Release(
            "r2000",
            "info",
            "http://example.com/release_link",
            "http://example.com/release_link.apk",
        )

        coEvery { releaseService.latest(any()) } returns release

        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isFoss = false,
                isNightly = true,
                commitCount = 1000,
                versionName = "",
                repository = "test",
            ),
        )

        (result as GetApplicationRelease.Result.NewUpdate).release shouldBe GetApplicationRelease.Result.NewUpdate(
            release,
        ).release
    }

    @Test
    fun `When has update expect new update`() = runTest {
        val release = Release(
            "v2.0.0",
            "info",
            "http://example.com/release_link",
            "http://example.com/release_link.apk",
        )

        coEvery { releaseService.latest(any()) } returns release

        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isFoss = false,
                isNightly = false,
                commitCount = 0,
                versionName = "v1.0.0",
                repository = "test",
            ),
        )

        (result as GetApplicationRelease.Result.NewUpdate).release shouldBe GetApplicationRelease.Result.NewUpdate(
            release,
        ).release
    }

    @Test
    fun `When has no update expect no new update`() = runTest {
        val release = Release(
            "v1.0.0",
            "info",
            "http://example.com/release_link",
            "http://example.com/release_link.apk",
        )

        coEvery { releaseService.latest(any()) } returns release

        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isFoss = false,
                isNightly = false,
                commitCount = 0,
                versionName = "v2.0.0",
                repository = "test",
            ),
        )

        result shouldBe GetApplicationRelease.Result.NoNewUpdate
    }

    @Test
    fun `When patch version is added expect new update`() = runTest {
        coEvery { releaseService.latest(any()) } returns release("1.0.1")

        getApplicationRelease.await(arguments("1.0")) shouldBe
            GetApplicationRelease.Result.NewUpdate(release("1.0.1"))
    }

    @Test
    fun `When older major has higher minor expect no new update`() = runTest {
        coEvery { releaseService.latest(any()) } returns release("1.5")

        getApplicationRelease.await(arguments("2.0")) shouldBe GetApplicationRelease.Result.NoNewUpdate
    }

    @Test
    fun `When release drops trailing zero expect no new update`() = runTest {
        coEvery { releaseService.latest(any()) } returns release("1.0")

        getApplicationRelease.await(arguments("1.0.0")) shouldBe GetApplicationRelease.Result.NoNewUpdate
    }

    private fun release(version: String) = Release(version, "info", "https://t.me/c/1", "https://t.me/c/1")

    private fun arguments(versionName: String) = GetApplicationRelease.Arguments(
        isFoss = false,
        isNightly = false,
        commitCount = 0,
        versionName = versionName,
        repository = "test",
    )
}
