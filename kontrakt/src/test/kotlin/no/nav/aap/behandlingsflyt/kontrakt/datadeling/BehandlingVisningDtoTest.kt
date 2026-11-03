package no.nav.aap.behandlingsflyt.kontrakt.datadeling

import com.fasterxml.jackson.databind.JsonNode
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.BehandlingHendelse.AlderLovvalgMedlemskap.Status
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.BehandlingHendelse.NavKontor
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class BehandlingVisningDtoTest {
    @Test
    fun `BehandlingVisningRequest roundtripper alle hendelsestyper med eksisterende navn som diskriminator`() {
        val hendelser = listOf(
            BehandlingHendelse.SøknadMottatt(
                sistEndret = LocalDateTime.of(2026, 10, 1, 10, 0),
                progresjon = BehandlingHendelseProgresjon.FULLFØRT,
            ),
            BehandlingHendelse.AlderLovvalgMedlemskap(
                sistEndret = LocalDateTime.of(2026, 10, 2, 10, 0),
                progresjon = BehandlingHendelseProgresjon.AKTIV,
                status = Status.MOTTATT_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER,
            ),
            BehandlingHendelse.NavKontor(
                sistEndret = LocalDateTime.of(2026, 10, 3, 10, 0),
                progresjon = BehandlingHendelseProgresjon.AKTIV,
                status = NavKontor.Status.PURRET_PÅ_LEGERERKLÆRING,
            ),
            BehandlingHendelse.Nay(
                sistEndret = LocalDateTime.of(2026, 10, 4, 10, 0),
                progresjon = BehandlingHendelseProgresjon.IKKE_PÅBEGYNT,
                status = BehandlingHendelse.Nay.Status.VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER,
            ),
            BehandlingHendelse.FerdigBehandlet(
                sistEndret = LocalDateTime.of(2026, 10, 5, 10, 0),
                progresjon = BehandlingHendelseProgresjon.FULLFØRT,
            ),
        )
        val request = BehandlingVisningRequest(
            ident = "12345678901",
            saksnummer = "123456",
            behandling = BehandlingVisningDto(
                behandlingReferanse = BehandlingReferanse(),
                avsluttet = false,
                behandlingType = BehandlingHendelseType.SØKNAD,
                hendelser = hendelser,
            ),
        )

        val json = DefaultJsonMapper.toJson(request)
        val roundtrippet = DefaultJsonMapper.fromJson<BehandlingVisningRequest>(json)
        val jsonHendelser = DefaultJsonMapper.fromJson<JsonNode>(json)["behandling"]["hendelser"]

        assertThat(roundtrippet).isEqualTo(request)
        assertThat(roundtrippet.behandling.hendelser).containsExactlyElementsOf(hendelser)
        assertThat(roundtrippet.behandling.hendelser[1])
            .isInstanceOf(BehandlingHendelse.AlderLovvalgMedlemskap::class.java)
        assertThat((roundtrippet.behandling.hendelser[1] as BehandlingHendelse.AlderLovvalgMedlemskap).status)
            .isEqualTo(Status.MOTTATT_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER)
        assertThat(roundtrippet.behandling.hendelser[2]).isInstanceOf(BehandlingHendelse.NavKontor::class.java)
        assertThat((roundtrippet.behandling.hendelser[2] as BehandlingHendelse.NavKontor).status)
            .isEqualTo(NavKontor.Status.PURRET_PÅ_LEGERERKLÆRING)

        assertThat(jsonHendelser.map { it["navn"].asText() }).containsExactly(
            "SØKNAD_MOTTATT",
            "ALDER_LOVVALG_MEDLEMSKAP",
            "NAV_KONTOR",
            "NAY",
            "FERDIG_BEHANDLET",
        )
        jsonHendelser.forEach { hendelse ->
            assertThat(hendelse.has("navn")).isTrue()
            assertThat(hendelse.has("@type")).isFalse()
            assertThat(hendelse.has("type")).isFalse()
        }
    }
}
