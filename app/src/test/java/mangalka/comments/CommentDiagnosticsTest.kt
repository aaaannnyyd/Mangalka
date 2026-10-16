package mangalka.comments

import android.content.Context
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.addSingleton

// Messages are localized resources; these tests check the classification, not the wording.
class CommentDiagnosticsTest {

    @BeforeEach
    fun setUp() {
        Injekt.addSingleton<Context>(mockk(relaxed = true))
    }

    @Test
    fun `http 403 is classified as forbidden with actionable message`() {
        val diagnostic = CommentHttpException(403, "https://example.test/comments").toCommentDiagnostic()

        assertEquals(CommentFailureKind.FORBIDDEN, diagnostic.kind)
        assertTrue(diagnostic.technicalDetail.orEmpty().contains("HTTP 403"))
    }

    @Test
    fun `target resolution failure is not presented as an empty thread`() {
        val diagnostic = CommentTargetResolutionException("mangalib", "branch 22 not found").toCommentDiagnostic()

        assertEquals(CommentFailureKind.TARGET_RESOLUTION, diagnostic.kind)
        assertTrue(diagnostic.technicalDetail.orEmpty().contains("branch 22"))
    }

    @Test
    fun `unsupported adapter is non retryable`() {
        val diagnostic = NoCommentAdapterException(42L).toCommentDiagnostic()

        assertEquals(CommentFailureKind.UNSUPPORTED, diagnostic.kind)
        assertFalse(diagnostic.retryable)
    }
}
