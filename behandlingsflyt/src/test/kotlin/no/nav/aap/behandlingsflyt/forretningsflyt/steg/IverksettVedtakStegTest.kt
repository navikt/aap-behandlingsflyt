package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.GosysService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.NavKontorPeriodeDto
import no.nav.aap.behandlingsflyt.flyt.AbstraktFlytOrkestratorTest
import no.nav.aap.behandlingsflyt.flyt.TestPersoner
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class IverksettVedtakStegTest : AbstraktFlytOrkestratorTest(AlleAvskruddUnleash::class) {

    @BeforeEach
    fun setup() {
        mockkConstructor(GosysService::class)
        every {
            anyConstructed<GosysService>().opprettOppgave(any(), any(), any(), any())
        } just Runs
    }

    @Test
    fun `virkingsdato på revurdert refusjon skal bruke første tidligere innvilgede vedtaksdato`() {
        val (sak, behandling) = sendInnFørsteSøknad(
            person = TestPersoner.STANDARD_PERSON(),
            mottattTidspunkt = LocalDateTime.now().minusDays(10)
        )
        // Avslag
        behandling
            .løsSykdom(vurderingGjelderFra = sak.rettighetsperiode.fom, erOppfylt = true)
            .løsBistand(fom = sak.rettighetsperiode.fom, erOppfylt = true, erBehovForArbeidsrettetTiltak = true)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(sak.rettighetsperiode.fom, false)
            .løsAndreStatligeYtelser()
            .løsForeslåVedtak()
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_AVSLAG)
            .medKontekst {
                val resultat = ResultatUtleder(repositoryProvider).utledResultatFørstegangsBehandling(behandling.id)
                assertThat(resultat).isEqualTo(Resultat.AVSLAG)
            }

        // Innvilget
        val revurdering = sak.opprettManuellRevurdering(
            listOf(Vurderingsbehov.HELHETLIG_VURDERING, Vurderingsbehov.OPPHOLDSKRAV)
        )
            .løsSykdom(vurderingGjelderFra = sak.rettighetsperiode.fom, erOppfylt = true)
            .løsBistand(fom = sak.rettighetsperiode.fom, erOppfylt = true, erBehovForArbeidsrettetTiltak = true)
            .løsSykdomsvurderingBrev()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(sak.rettighetsperiode.fom, true)
            .løsAndreStatligeYtelser()
            .løsForeslåVedtak()
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_ENDRING)

        assertThat(revurdering.status()).isEqualTo(Status.AVSLUTTET)

        verify(atLeast = 1) {
            anyConstructed<GosysService>().opprettOppgave(
                aktivIdent = sak.person.aktivIdent(),
                bestillingReferanse = any(),
                behandlingId = revurdering.id,
                navKontor = NavKontorPeriodeDto(
                    enhetsNummer = "Peppas Crib",
                    virkingsdato = revurdering.opprettetTidspunkt.toLocalDate(),
                    vedtaksdato = revurdering.opprettetTidspunkt.toLocalDate().minusDays(1)
                )
            )
        }
    }
}