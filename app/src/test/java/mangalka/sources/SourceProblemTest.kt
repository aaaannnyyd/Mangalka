package mangalka.sources

import android.content.Context
import eu.kanade.presentation.util.SourceProblem.Kind
import eu.kanade.presentation.util.toSourceProblem
import eu.kanade.tachiyomi.network.HttpException
import io.mockk.mockk
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.data.source.NoResultsException
import java.io.IOException
import java.net.SocketTimeoutException

class SourceProblemTest {

    private val context = mockk<Context>(relaxed = true)

    private fun kindOf(error: Throwable) = with(context) { error.toSourceProblem() }.kind

    @Test
    fun `missing field from a changed site api asks for an extension update`() {
        @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
        val error = MissingFieldException("baseUrl", "eu.kanade.tachiyomi.extension.en.comix.Pages")
        assertEquals(Kind.SourceChanged, kindOf(error))
        assertTrue(Kind.SourceChanged.extensionUpdateMayHelp)
    }

    @Test
    fun `parsing failures wrapped by the loader are still recognised`() {
        assertEquals(Kind.SourceChanged, kindOf(IOException("load", SerializationException("bad json"))))
        assertEquals(Kind.SourceChanged, kindOf(IndexOutOfBoundsException()))
    }

    @Test
    fun `http codes map to blocked, gone and down`() {
        assertEquals(Kind.Blocked, kindOf(HttpException(403)))
        assertEquals(Kind.NotFound, kindOf(HttpException(404)))
        assertEquals(Kind.ServerDown, kindOf(HttpException(503)))
    }

    @Test
    fun `plain extension exception is a source error, timeouts are not source side`() {
        assertEquals(Kind.SourceError, kindOf(Exception("Could not find environment bundle")))
        assertEquals(Kind.Timeout, kindOf(SocketTimeoutException()))
        assertFalse(Kind.Timeout.isSourceSide)
    }

    @Test
    fun `empty search results stay a normal message`() {
        assertEquals(Kind.Other, kindOf(NoResultsException()))
    }
}
