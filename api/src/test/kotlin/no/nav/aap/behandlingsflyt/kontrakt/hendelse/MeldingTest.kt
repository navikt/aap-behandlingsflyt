package no.nav.aap.behandlingsflyt.kontrakt.hendelse

import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MeldingTest {
    @Test
    fun `fsdf sdf `() {
        val x = SøknadV1(
            student = SøknadStudentDto(
                erStudent = "ja",
                kommeTilbake = "ja"
            ),
            yrkesskade = "ja",
            oppgitteBarn = OppgitteBarn(
                identer = setOf("334")
            ),
            nyttFelt = 2,
//            endaEtFelt = "xxx"
        )

        assertThat(DefaultJsonMapper.toJson(x)).isEqualTo(1212)

        assertThat(DefaultJsonMapper.fromJson<Melding>(DefaultJsonMapper.toJson(x))).isEqualTo(123)
    }
}