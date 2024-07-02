package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import no.nav.aap.json.DefaultJsonMapper
import no.nav.aap.pdl.PdlPersonNavnDataResponse
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
}