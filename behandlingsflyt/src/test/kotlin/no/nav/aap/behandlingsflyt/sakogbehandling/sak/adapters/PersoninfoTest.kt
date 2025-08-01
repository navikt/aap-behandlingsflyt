package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlPersonNavnDataResponse
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PersoninfoTest {
    @Test
    fun `skal mappe rett`() {
        val json = """
            {"data":{"hentPerson":{"navn":[{"fornavn":"LYKKELIG","mellomnavn":"PÅTROPPENDE","etternavn":"KLAUSTROFOBI"}]}}}
        """.trimIndent()

        val respons = DefaultJsonMapper.fromJson<PdlPersonNavnDataResponse>(json)

        assertThat(respons).isNotNull
    }

    @Test
    fun `konkatenerer navn riktig`() {
        val info = Personinfo(
            Ident("1234"),
            fornavn = "Leonardo",
            mellomnavn = "Igorsson",
            etternavn = "da Vinci",
        )

        assertThat(info.fulltNavn()).isEqualTo("Leonardo Igorsson da Vinci")
    }

    @Test
    fun `når navn er null er fullt navn Ukjent`() {
        val info = Personinfo(
            Ident("1234"),
            fornavn = null,
            mellomnavn = null,
            etternavn = null,
        )

        assertThat(info.fulltNavn()).isEqualTo("Ukjent")
    }
}