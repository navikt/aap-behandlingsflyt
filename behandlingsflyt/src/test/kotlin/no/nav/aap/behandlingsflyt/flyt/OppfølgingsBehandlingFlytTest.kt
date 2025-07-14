package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOppfølgingNAYLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VentPåOppfølgingLøsning
import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.KonsekvensAvOppfølging
import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.OppfølgingsoppgaveGrunnlagDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.flyt.internals.DokumentMottattPersonHendelse
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.HvemSkalFølgeOpp
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OppfølgingsoppgaveV0
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class OppfølgingsBehandlingFlytTest : AbstraktFlytOrkestratorTest() {
    @Test
    fun `opprette oppfølgingsbehandling`() {
        val sak = happyCaseFørstegangsbehandling()
        val førstegangsbehandling = hentNyesteBehandlingForSak(sak.id)

        val ident = sak.person.aktivIdent()
        val periode = sak.rettighetsperiode

        val oppfølgingsbehandling = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                referanse = InnsendingReferanse(
                    InnsendingReferanse.Type.BEHANDLING_REFERANSE,
                    UUID.randomUUID().toString(),
                ),
                mottattTidspunkt = LocalDateTime.now().minusMonths(3),
                strukturertDokument = StrukturertDokument(
                    OppfølgingsoppgaveV0(
                        datoForOppfølging = LocalDate.now().plusDays(1),
                        hvaSkalFølgesOpp = "noe",
                        hvemSkalFølgeOpp = HvemSkalFølgeOpp.NasjonalEnhet(),
                        reserverTilBruker = "MEGSELV"
                    )
                ),
                periode = periode
            )
        )
            .medKontekst {
                assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.OppfølgingsBehandling)
                assertThat(behandling.referanse).isNotEqualTo(førstegangsbehandling.referanse)
                assertThat(ventebehov.map { it.definisjon }).containsOnly(Definisjon.VENT_PÅ_OPPFØLGING)
            }
            .løsAvklaringsBehov(VentPåOppfølgingLøsning())
            .medKontekst {
                assertThat(behandling.aktivtSteg())
                    .describedAs { "Forventer at steget har endret seg" }
                    .isNotEqualTo(StegType.START_OPPFØLGINGSBEHANDLING)
            }
            .løsAvklaringsBehov(
                AvklarOppfølgingNAYLøsning(
                    OppfølgingsoppgaveGrunnlagDto(
                        konsekvensAvOppfølging = KonsekvensAvOppfølging.OPPRETT_VURDERINGSBEHOV,
                        opplysningerTilRevurdering = listOf(ÅrsakTilBehandling.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND),
                        årsak = "dddd"
                    )
                )
            )
            .medKontekst {
                assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)
            }

        val opprettetBehandling =
            hentNyesteBehandlingForSak(
                oppfølgingsbehandling.sakId,
                listOf(TypeBehandling.Revurdering)
            )

        util.ventPåSvar(opprettetBehandling)

        opprettetBehandling.medKontekst {
            assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
            assertThat(åpneAvklaringsbehov.map { it.definisjon }).containsOnly(Definisjon.AVKLAR_SYKDOM)
        }
    }
}