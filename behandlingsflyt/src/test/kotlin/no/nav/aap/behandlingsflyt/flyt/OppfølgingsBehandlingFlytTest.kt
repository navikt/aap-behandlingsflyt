package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOppfølgingNAYLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VentPåOppfølgingLøsning
import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.KonsekvensAvOppfølging
import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.OppfølgingsoppgaveGrunnlagDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.flyt.internals.DokumentMottattPersonHendelse
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.HvemSkalFølgeOpp
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OppfølgingsoppgaveV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Opprinnelse
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.komponenter.dbconnect.transaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class OppfølgingsBehandlingFlytTest : AbstraktFlytOrkestratorTest(FakeUnleash::class) {
    @Test
    fun `opprette oppfølgingsbehandling`() {
        val sak = happyCaseFørstegangsbehandling()
        val førstegangsbehandling = hentSisteOpprettedeBehandlingForSak(sak.id)

        val ident = sak.person.aktivIdent()
        val periode = sak.rettighetsperiode

        val oppfølgingsbehandling = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                referanse = InnsendingReferanse(
                    InnsendingReferanse.Type.SAKSBEHANDLER_KELVIN_REFERANSE,
                    UUID.randomUUID().toString(),
                ),
                mottattTidspunkt = LocalDateTime.now().minusMonths(3),
                strukturertDokument = StrukturertDokument(
                    OppfølgingsoppgaveV0(
                        datoForOppfølging = LocalDate.now().plusDays(1),
                        hvaSkalFølgesOpp = "noe",
                        hvemSkalFølgeOpp = HvemSkalFølgeOpp.NasjonalEnhet,
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
                        opplysningerTilRevurdering = listOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND),
                        årsak = "dddd"
                    )
                )
            )
            .medKontekst {
                assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)
            }

        val opprettetBehandling =
            hentSisteOpprettedeBehandlingForSak(
                oppfølgingsbehandling.sakId,
                listOf(TypeBehandling.Revurdering)
            )

        motor.kjørJobber()

        opprettetBehandling.medKontekst {
            assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
            assertThat(åpneAvklaringsbehov.map { it.definisjon }).containsOnly(Definisjon.AVKLAR_SYKDOM)
        }
    }

    @Test
    fun `Opprett oppfølgningsoppgave med opprinnelse`() {

        val sak = happyCaseFørstegangsbehandling()
        val førstegangsbehandling = hentSisteOpprettedeBehandlingForSak(sak.id)

        val ident = sak.person.aktivIdent()
        val periode = sak.rettighetsperiode
        sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                referanse = InnsendingReferanse(
                    InnsendingReferanse.Type.SAKSBEHANDLER_KELVIN_REFERANSE,
                    UUID.randomUUID().toString(),
                ),
                mottattTidspunkt = LocalDateTime.now().minusMonths(3),
                strukturertDokument = StrukturertDokument(
                    OppfølgingsoppgaveV0(
                        datoForOppfølging = LocalDate.now().plusDays(1),
                        hvaSkalFølgesOpp = "noe",
                        hvemSkalFølgeOpp = HvemSkalFølgeOpp.NasjonalEnhet,
                        reserverTilBruker = "MEGSELV",
                        opprinnelse = Opprinnelse(
                            behandlingsreferanse = førstegangsbehandling.referanse.toString(),
                            avklaringsbehovKode = AvklaringsbehovKode.`5028`.toString()
                        )
                    )
                ),
                periode = periode
            )
        )
        val oppretteOppfølgingsoppgave = dataSource.transaction { connection ->

            val repositoryProvider = postgresRepositoryRegistry.provider(connection)
            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()

            val alleBehandlinger =
                behandlingRepository.hentAlleFor(sak.id, listOf(TypeBehandling.OppfølgingsBehandling))

            alleBehandlinger.mapNotNull { behandling ->
                val dokument = MottaDokumentService(repositoryProvider.provide())
                    .hentOppfølgingsBehandlingDokument(behandlingId = behandling.id)

                if (dokument != null) {
                    Pair(behandling.id, dokument)
                } else {
                    null
                }
            }
        }
        assertThat(oppretteOppfølgingsoppgave.size).isEqualTo(1)
        assertThat(oppretteOppfølgingsoppgave.first().second.opprinnelse).isNotNull
        assertThat(oppretteOppfølgingsoppgave.first().second.opprinnelse!!.behandlingsreferanse).isEqualTo(
            førstegangsbehandling.referanse.toString()
        )
        assertThat(oppretteOppfølgingsoppgave.first().second.opprinnelse!!.avklaringsbehovKode).isEqualTo(
            AvklaringsbehovKode.`5028`.toString()
        )

    }

}