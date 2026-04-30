package no.nav.aap.behandlingsflyt

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.utils.KryptertString
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@Fakes
class PersonApiTest : BaseApiTest() {
    private val testNøkkel = "test-nokkel-for-kelvin-id-32byte".toByteArray()
    private val codec = KryptertString(testNøkkel)

    @Test
    fun `returnerer aktiv ident for kjent person`() {
        val ident = "12345678901"

        testApplication {
            installApplication {
                personApi(codec)
            }

            val response = createClient().post("/api/person/ident") {
                header("Authorization", "Bearer ${getToken().token()}")
                contentType(ContentType.Application.Json)
                setBody(PersonIdentRequest(kryptertIdent = codec.encode(ident)))  // <-- encode
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.body<PersonIdentResponse>()).isEqualTo(
                PersonIdentResponse(ident = ident)
            )
        }
    }


}
