package no.nav.aap.behandlingsflyt.kontrakt.hendelse

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class BehandlingFlytStoppetHendelseTest {

    @Test
    fun `Skal transformeres til json og tilbake til objekt`() {
        val hendelse = BehandlingFlytStoppetHendelse(
            personIdent = "1234123411",
            saksnummer = Saksnummer("ASDF"),
            referanse = BehandlingReferanse(),
            behandlingType = TypeBehandling.Førstegangsbehandling,
            status = Status.UTREDES,
            avklaringsbehov = listOf(
                AvklaringsbehovHendelseDto(
                    definisjon = DefinisjonDTO(
                        type = Definisjon.AVKLAR_SYKDOM.kode,
                        behovType = Definisjon.AVKLAR_SYKDOM.type,
                        løsesISteg = Definisjon.AVKLAR_SYKDOM.løsesISteg
                    ),
                    status = no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.OPPRETTET,
                    endringer = listOf(
                        EndringDTO(
                            status = no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.OPPRETTET,
                            tidsstempel = LocalDateTime.now(),
                            frist = null,
                            endretAv = "Kelvin"
                        )
                    )
                )
            ),
            opprettetTidspunkt = LocalDateTime.now(),
            hendelsesTidspunkt = LocalDateTime.now(),
            versjon = "1"
        )

        val hendelseJson = DefaultJsonMapper.toJson(hendelse)

        val parsedHendelse = DefaultJsonMapper.fromJson<BehandlingFlytStoppetHendelse>(hendelseJson)

        assertThat(parsedHendelse).isEqualTo(hendelse)
    }
}