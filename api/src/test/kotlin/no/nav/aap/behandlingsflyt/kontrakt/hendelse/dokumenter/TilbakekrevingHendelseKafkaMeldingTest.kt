package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TilbakekrevingHendelseKafkaMeldingTest {

    @Test
    fun `Kan parse hendelse`() {
        val json = """
            {
                "hendelsestype":"behandling_endret",
                "eksternFagsakId":"4NVM35C",
                "hendelseOpprettet":"2026-05-19T13:11:50.703204279+02:00",
                "eksternBehandlingId":"f2b941c9-458b-4ae4-836a-580323f8ffb9",
                "tilbakekreving":{
                    "behandlingId":"f97f2764-0be5-46c0-aa11-74daf70420e9",
                    "sakOpprettet":"2026-02-13T23:26:09.986126+01:00",
                    "venter":{
                        "grunn":"AVVENTER_BRUKERUTTALELSE",
                        "gjenopptas":"2026-06-01"
                    },
                    "varselSendt":"2026-05-11",
                    "behandlingsstatus":"TIL_BEHANDLING",
                    "forrigeBehandlingsstatus":"TIL_BEHANDLING",
                    "totaltFeilutbetaltBeløp":"81302.00",
                    "saksbehandlingURL":"https://tilbakekreving.intern.nav.no/fagsystem/AAP/fagsak/4NVM35C/behandling/f97f2764-0be5-46c0-aa11-74daf70420e9",
                    "fullstendigPeriode":{
                        "fom":"2025-11-19",
                        "tom":"2026-02-06"
                    }
                },
                "versjon":1
            }            
        """.trimIndent()

        val melding = DefaultJsonMapper.fromJson<TilbakekrevingHendelseKafkaMelding>(json)

        assertThat(melding.hendelsestype).isEqualTo("behandling_endret")
        assertThat(melding.tilbakekreving!!.varselSendt).isEqualTo(LocalDate.parse("2026-05-11"))
        assertThat(melding.tilbakekreving!!.venter!!.grunn).isEqualTo(TilbakekrevingGrunn.AVVENTER_BRUKERUTTALELSE)
        assertThat(melding.tilbakekreving!!.venter!!.gjenopptas).isEqualTo(LocalDate.parse("2026-06-01"))
    }


}