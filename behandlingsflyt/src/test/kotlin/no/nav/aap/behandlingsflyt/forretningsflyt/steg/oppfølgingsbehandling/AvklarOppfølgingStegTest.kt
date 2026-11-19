package no.nav.aap.behandlingsflyt.forretningsflyt.steg.oppfølgingsbehandling

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.KonsekvensAvOppfølging
import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.OppfølgingsoppgaveGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantVentebehov
import no.nav.aap.behandlingsflyt.flyt.steg.Ventebehov
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.HvemSkalFølgeOpp
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OppfølgingsoppgaveV0
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.DummyBehandlingHendelseServiceFactory
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryFlytJobbRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMottattDokumentRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryOppfølgingsBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.behandlingsflyt.test.minimalGatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AvklarOppfølgingStegTest {
    private val sak = opprettInMemorySak()
    private val behandling = InMemoryBehandlingRepository.opprettBehandling(
        sakId = sak.id,
        typeBehandling = TypeBehandling.OppfølgingsBehandling,
        forrigeBehandlingId = null,
        vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
            listOf(VurderingsbehovMedPeriode(Vurderingsbehov.OPPFØLGINGSOPPGAVE)),
            ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE
        )
    )
    private val datoForOppfølging = LocalDate.now().plusDays(7)
    private val gatewayProvider = minimalGatewayProvider {
        register<DummyBehandlingHendelseServiceFactory>()
    }

    @BeforeEach
    fun settOppDokument() {
        InMemoryMottattDokumentRepository.lagre(
            MottattDokument(
                referanse = InnsendingReferanse(
                    InnsendingReferanse.Type.SAKSBEHANDLER_KELVIN_REFERANSE,
                    UUID.randomUUID().toString()
                ),
                sakId = sak.id,
                behandlingId = behandling.id,
                mottattTidspunkt = LocalDateTime.now(),
                type = InnsendingType.OPPFØLGINGSOPPGAVE,
                kanal = Kanal.DIGITAL,
                strukturertDokument = StrukturertDokument(
                    OppfølgingsoppgaveV0(
                        datoForOppfølging = datoForOppfølging,
                        hvemSkalFølgeOpp = HvemSkalFølgeOpp.NasjonalEnhet,
                        hvaSkalFølgesOpp = "...",
                        opprettetAv = null,
                        reserverTilBruker = null
                    )
                )
            )
        )
    }

    @Test
    fun `om konsevens av oppfølging er OPPRETT_VURDERINGSBEHOV, så opprettes behandling med riktig årsak`() {
        val grunnlag = OppfølgingsoppgaveGrunnlag(
            konsekvensAvOppfølging = KonsekvensAvOppfølging.OPPRETT_VURDERINGSBEHOV,
            opplysningerTilRevurdering = listOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND),
            årsak = "...",
            vurdertAv = "ff"
        )
        val (steg, kontekst) = settOppTilstand(grunnlag)

        steg.utfør(kontekst)

        val ytelsesbehandling = InMemoryBehandlingRepository.hentAlleIkkeAvbrutteYtelsesbehandlinger(sak.id).single()
        assertThat(ytelsesbehandling.vurderingsbehov().map { it.type })
            .containsExactly(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND)
        assertThat(ytelsesbehandling.årsakTilOpprettelse).isEqualTo(ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE)
        val jobb = InMemoryFlytJobbRepository.hentJobberForSak(sak.id.toLong()).single()
        assertThat(jobb.type()).isEqualTo(ProsesserBehandlingJobbUtfører.type)
        assertThat(jobb.behandlingIdOrNull()).isEqualTo(ytelsesbehandling.id.toLong())
    }

    @Test
    fun `om konsekvens av oppfølging er INGEN, så fullføres steget`() {
        val grunnlag = OppfølgingsoppgaveGrunnlag(
            konsekvensAvOppfølging = KonsekvensAvOppfølging.INGEN,
            opplysningerTilRevurdering = emptyList(),
            årsak = "...",
            vurdertAv = "ff"
        )
        val (steg, kontekst) = settOppTilstand(grunnlag)

        steg.utfør(kontekst)

        assertThat(InMemoryBehandlingRepository.hentAlleIkkeAvbrutteYtelsesbehandlinger(sak.id)).isEmpty()
        assertThat(InMemoryFlytJobbRepository.hentJobberForSak(sak.id.toLong())).isEmpty()
    }

    @Test
    fun `om informasjon mangler, lag avklaringsbehov`() {
        val (steg, kontekst) = settOppTilstand(null)

        steg.utfør(kontekst)
        val avklaringsbehovene = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
        assertThat(
            avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_OPPFØLGINGSBEHOV_NAY)?.status()
        ).isEqualTo(no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.OPPRETTET)

        assertThat(InMemoryBehandlingRepository.hentAlleIkkeAvbrutteYtelsesbehandlinger(sak.id)).isEmpty()
        assertThat(InMemoryFlytJobbRepository.hentJobberForSak(sak.id.toLong())).isEmpty()
    }

    @Test
    fun `Hvis ikke behandlingen har vært på vent før skal steget returnere FantVentebehov`() {
        val (steg, kontekst) = settOppTilstand(null)

        val resultat = steg.utfør(kontekst)

        assertThat(resultat).isEqualTo(
            FantVentebehov(
                ventebehov = Ventebehov(
                    definisjon = Definisjon.VENT_PÅ_OPPFØLGING_NY,
                    frist = datoForOppfølging,
                    grunn = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER
                )
            )
        )
        val avklaringsbehovene = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
        assertThat(
            avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_OPPFØLGINGSBEHOV_NAY)?.status()
        ).isEqualTo(no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.OPPRETTET)
    }

    private fun settOppTilstand(
        grunnlag: OppfølgingsoppgaveGrunnlag?
    ): Pair<BehandlingSteg, FlytKontekstMedPerioder> {
        if (grunnlag != null) {
            InMemoryOppfølgingsBehandlingRepository.lagre(behandling.id, grunnlag)
        }
        val steg = AvklarOppfølgingSteg.konstruer(inMemoryRepositoryProvider, gatewayProvider)
        val kontekst = flytKontekstMedPerioder {
            this.behandling = this@AvklarOppfølgingStegTest.behandling
            this.rettighetsperiode = Periode(13 februar 1989, 13 mars 2025)
            this.vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle().toSet()
        }
        return Pair(steg, kontekst)
    }
}
