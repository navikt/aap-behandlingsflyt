package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TilbakekrevingHendelseKafkaMeldingTest {

    @Test
    fun `Kan parse melding til objekt når varselSendt er timestamp`() {
        val json = """
            {"hendelsestype":"behandling_endret","versjon":1,"eksternFagsakId":"4oX293K","hendelseOpprettet":"2025-10-28T10:04:25.851546502","eksternBehandlingId":null,"sakOpprettet":"2025-10-28T10:04:25.176290819","varselSendt":"2025-10-28T10:04:25.851546502","behandlingsstatus":"OPPRETTET","totaltFeilutbetaltBeløp":"7104.00","saksbehandlingURL":"https://tilbakekreving.ansatt.dev.nav.no/fagsystem/AAP/fagsak/4oX293K/behandling/91e3a861-e79d-408d-9811-906ec34068e1","fullstendigPeriode":{"fom":"2025-10-06","tom":"2025-10-17"}}
        """.trimIndent()

        val melding = DefaultJsonMapper.fromJson<TilbakekrevingHendelseKafkaMelding>(json)

        assertThat(melding.hendelsestype).isEqualTo("behandling_endret")
        assertThat(melding.varselSendt).isNotNull()
    }


    @Test
    fun `Kan parse melding til objekt når varselSendt er date`() {
        val json = """
            {"hendelsestype":"behandling_endret","versjon":1,"eksternFagsakId":"4oX293K","hendelseOpprettet":"2025-10-28T10:04:25.851546502","eksternBehandlingId":null,"sakOpprettet":"2025-10-28T10:04:25.176290819","varselSendt":"2025-10-28","behandlingsstatus":"OPPRETTET","totaltFeilutbetaltBeløp":"7104.00","saksbehandlingURL":"https://tilbakekreving.ansatt.dev.nav.no/fagsystem/AAP/fagsak/4oX293K/behandling/91e3a861-e79d-408d-9811-906ec34068e1","fullstendigPeriode":{"fom":"2025-10-06","tom":"2025-10-17"}}
        """.trimIndent()

        val melding = DefaultJsonMapper.fromJson<TilbakekrevingHendelseKafkaMelding>(json)

        assertThat(melding.hendelsestype).isEqualTo("behandling_endret")
        assertThat(melding.varselSendt).isNotNull()
    }


}