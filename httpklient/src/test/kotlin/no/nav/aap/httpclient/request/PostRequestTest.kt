package no.nav.aap.httpclient.request

import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PostRequestTest {

    @Test
    fun contenttypestring() {
        val request = PostRequest(body = "asdf")

        assertThat(request.contentType()).isEqualTo("application/json")
    }
}