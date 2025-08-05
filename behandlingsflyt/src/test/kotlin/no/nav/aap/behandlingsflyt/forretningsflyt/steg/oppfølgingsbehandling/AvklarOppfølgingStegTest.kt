package no.nav.aap.behandlingsflyt.forretningsflyt.steg.oppfølgingsbehandling

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.KonsekvensAvOppfølging
import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.OppfølgingsBehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.OppfølgingsoppgaveGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.BehandletOppfølgingsOppgave
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.HvemSkalFølgeOpp
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
class AvklarOppfølgingStegTest {

    @MockK
    lateinit var oppfølgingsBehandlingRepository: OppfølgingsBehandlingRepository

    @MockK(relaxed = true)
    lateinit var låsRepository: TaSkriveLåsRepository

    @MockK(relaxed = true)
    lateinit var prosesserBehandling: ProsesserBehandlingService

    @MockK
    lateinit var sakOgBehandlingService: SakOgBehandlingService

    @MockK
    lateinit var mottaDokumentService: MottaDokumentService

    val behandling = Behandling(
        id = BehandlingId(1),
        forrigeBehandlingId = null,
        referanse = BehandlingReferanse(UUID.randomUUID()),
        sakId = SakId(1),
        typeBehandling = TypeBehandling.Førstegangsbehandling,
        årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
        status = Status.UTREDES,
        vurderingsbehov = listOf(),
        versjon = 0
    )

    @BeforeEach
    fun setup() {
        every { sakOgBehandlingService.finnEllerOpprettBehandling(any<SakId>(), any(), any()) } returns behandling

        every { mottaDokumentService.hentOppfølgingsBehandlingDokument(any())} returns BehandletOppfølgingsOppgave(
            datoForOppfølging = LocalDate.now(),
            hvemSkalFølgeOpp = HvemSkalFølgeOpp.NasjonalEnhet,
            hvaSkalFølgesOpp = "...",
            reserverTilBruker = null
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

        val res = steg.utfør(kontekst)

        assertThat(res).isEqualTo(Fullført)

        verify {
            prosesserBehandling.triggProsesserBehandling(behandling.sakId, behandling.id)
            sakOgBehandlingService.finnEllerOpprettBehandling(
                behandling.sakId,
                listOf(VurderingsbehovMedPeriode(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND)),
                ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE
            )
        }
    }

    @Test
    fun `om konsekvens av oppfølging er INGEN, så fullføres steget`() {
        val grunnlag = OppfølgingsoppgaveGrunnlag(
            konsekvensAvOppfølging = KonsekvensAvOppfølging.INGEN,
            opplysningerTilRevurdering = listOf(),
            årsak = "...",
            vurdertAv = "ff"
        )
        val (steg, kontekst) = settOppTilstand(grunnlag)

        val res = steg.utfør(kontekst)

        assertThat(res).isEqualTo(Fullført)

        verify(exactly = 0) {
            prosesserBehandling.triggProsesserBehandling(behandling.sakId, behandling.id)
            sakOgBehandlingService.finnEllerOpprettBehandling(
                behandling.sakId,
                listOf(VurderingsbehovMedPeriode(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND)),
                ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE
            )
        }
    }

    @Test
    fun `om informasjon mangler, lag avklaringsbehov`() {
        val (steg, kontekst) = settOppTilstand(null)

        val res = steg.utfør(kontekst)

        assertThat(res).isEqualTo(FantAvklaringsbehov(Definisjon.AVKLAR_OPPFØLGINGSBEHOV_NAY))

        verify(exactly = 0) {
            prosesserBehandling.triggProsesserBehandling(behandling.sakId, behandling.id)
            sakOgBehandlingService.finnEllerOpprettBehandling(
                behandling.sakId,
                listOf(VurderingsbehovMedPeriode(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND)),
                ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE
            )
        }
    }

    private fun settOppTilstand(
        grunnlag: OppfølgingsoppgaveGrunnlag?
    ): Pair<AvklarOppfølgingSteg, FlytKontekstMedPerioder> {
        every { oppfølgingsBehandlingRepository.hent(any()) } returns grunnlag

        val steg = AvklarOppfølgingSteg(
            oppfølgingsBehandlingRepository = oppfølgingsBehandlingRepository,
            sakOgBehandlingService = sakOgBehandlingService,
            låsRepository = låsRepository,
            prosesserBehandling = prosesserBehandling,
            mottaDokumentService = mottaDokumentService,
        )

        val kontekst = FlytKontekstMedPerioder(
            sakId = behandling.sakId,
            behandlingId = behandling.id,
            forrigeBehandlingId = null,
            behandlingType = behandling.typeBehandling(),
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            rettighetsperiode = Periode(13 februar 1989, 13 mars 2025),
            vurderingsbehov = Vurderingsbehov.alle().toSet()
        )
        return Pair(steg, kontekst)
    }


}