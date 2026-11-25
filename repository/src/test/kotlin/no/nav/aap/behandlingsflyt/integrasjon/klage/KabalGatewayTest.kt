package no.nav.aap.behandlingsflyt.integrasjon.klage

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.Adresse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.FullmektigVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.NavnOgAdresse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Opprettholdes
import no.nav.aap.behandlingsflyt.help.person
import no.nav.aap.behandlingsflyt.integrasjon.kabal.KabalGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.test.FakeServers
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Fakes
class KabalGatewayTest {

    @Test
    fun `oversender alle opprettholdte hjemler med riktige Kabal-koder`() {
        val behandlingsreferanse = BehandlingReferanse(UUID.randomUUID())

        KabalGateway().oversendTilAndreinstans(
            klagenGjelder = person(),
            behandlingsreferanse = behandlingsreferanse,
            saksbehandlersEnhet = "0301",
            kravDato = LocalDate.now(),
            saksnummer = Saksnummer("1"),
            klageresultat = Opprettholdes(
                vilkårSomSkalOpprettholdes = listOf(
                    Hjemmel.FOLKETRYGDLOVEN_11_5,
                    Hjemmel.FOLKETRYGDLOVEN_22_7,
                    Hjemmel.FOLKETRYGDLOVEN_22_8,
                    Hjemmel.FOLKETRYGDLOVEN_22_14_FJERDE_LEDD
                )
            ),
            kommentar = "Opprettholder klagen",
            fullmektig = null
        )

        assertThat(FakeServers.hentOversendteKabalHjemler(behandlingsreferanse.referanse.toString()))
            .containsExactly("FTRL_11_5", "FTRL_22_7", "FTRL_22_8", "FTRL_22_14")
    }

    @Test
    fun kanOversendeTilKabal() {
        assertDoesNotThrow {
            KabalGateway().oversendTilAndreinstans(
                klagenGjelder = person(),
                behandlingsreferanse = BehandlingReferanse(UUID.randomUUID()),
                saksbehandlersEnhet = "0301",
                kravDato = LocalDate.now(),
                saksnummer = Saksnummer("1"),
                klageresultat = Opprettholdes(
                    vilkårSomSkalOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5)
                ),
                kommentar = "kommenentar",
                fullmektig = FullmektigVurdering(
                    harFullmektig = true,
                    fullmektigIdent = null,
                    fullmektigNavnOgAdresse = NavnOgAdresse(
                        navn = "Ola Nordmann",
                        adresse = Adresse(
                            adresselinje1 = "Gateveien 1",
                            postnummer = "1234",
                            poststed = "Oslo",
                            landkode = "NO"
                        )
                    ),
                    vurdertAv = Bruker("Saksbehandler"),
                    opprettet = Instant.now(),
                )
            )
        }
    }
}