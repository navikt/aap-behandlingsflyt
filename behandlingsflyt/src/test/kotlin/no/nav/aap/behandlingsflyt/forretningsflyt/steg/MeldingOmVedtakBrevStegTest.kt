package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Endring
import no.nav.aap.behandlingsflyt.behandling.brev.BrevUtlederService
import no.nav.aap.behandlingsflyt.behandling.brev.VedtakAktivitetsplikt11_7
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class MeldingOmVedtakBrevStegTest {

    val brevbestillingService = mockk<BrevbestillingService>()
    val brevUtlederService = mockk<BrevUtlederService>()
    val trekkKlageService = mockk<TrekkKlageService>()
    val behandlingRepository = InMemoryBehandlingRepository
    val avklaringsbehovRepository = InMemoryAvklaringsbehovRepository
    val avklaringsbehovService = AvklaringsbehovService(inMemoryRepositoryProvider)

    @BeforeEach
    fun setup() {
        every { trekkKlageService.klageErTrukket(any()) } returns false
        every { brevUtlederService.utledBehovForMeldingOmVedtak(any()) } returns VedtakAktivitetsplikt11_7
        every { brevbestillingService.bestillV2(any(), any(), any(), any()) } returns UUID.randomUUID()
        every { brevbestillingService.erAlleBestillingerOmVedtakIEndeTilstand(any()) } returns true
        every { brevbestillingService.harBestillingOmVedtak(any()) } returns false
    }


    /**
     * Vurderinger rundt MeldingOmVedtakBrevSteg.utfør() og AvklaringsbehovService
     *   BrevSteg har per idag ingen periodiseringsbehov
     *   BrevSteg krever by-design manuelt avklaringsbehov ved brevbehov, dvs. vedtakBehøverVurdering gitt brevBehov og ingen klage trukket
     *   BrevSteg kan per i dag ikke tilbakestilles da behandlingsflyten har nådd stegstatus IVERKSETTES. Nåværende
     *   tilbakestillGrunnlag() logikk er delivs komplett (se AAP-1676 for manglende logikk ifm. tilbakestillGrunnlag)
     *
     *   Forventa runder i BrevSteg og oppdateringer av avklaringsbehov for førstegangsbehandling:
     *     Runde-1:
     *       - oppdaterAvklaringsbehov() i BrevSteg legger til nytt SKRIV_VEDTAKSBREV avklaringsbehov med status OPPRETTET, da erTilstrekkeligVurdert er false
     *       - BrevSteg utfører bestillBrev()
     *     Runde-2:
     *       - manuell avklaring resulterer i ny status=AVSLUTTET for avklaringsbehov SKRIV_VEDTAKSBRE
     *       - oppdaterAvklaringsbehov() i BrevSteg etter manuell avklaring ivaretar status AVSLUTTET, da erTilstrekkeligVurdert er true
     *       - BrevSteg utfører ikke bestillBrev()
     */
    @Test
    fun `Runder i BrevSteg ved førstegangsbehandling resulterer i opprettet og avsluttet avklaringsbehov samt brevbestilling`() {
        val behandling = InMemoryBehandlingRepository.opprettBehandling(
            sakId = SakId(1L),
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(
                    VurderingsbehovMedPeriode(
                        Vurderingsbehov.MOTTATT_SØKNAD,
                        Periode(LocalDate.now().minusDays(1), LocalDate.now().plusYears(1))
                    )
                ),
                årsak = ÅrsakTilOpprettelse.SØKNAD,
                opprettet = LocalDateTime.now(),
                beskrivelse = "unit-test"
            )
        )
        val kontekst = FlytKontekstMedPerioder(
            sakId = behandling.sakId,
            behandlingId = behandling.id,
            behandlingType = behandling.typeBehandling(),
            forrigeBehandlingId = behandling.forrigeBehandlingId,
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            rettighetsperiode = Periode(LocalDate.now().minusDays(1), LocalDate.now().plusYears(1)),
            vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTTATT_SØKNAD)
        )
        val steg = MeldingOmVedtakBrevSteg(
            brevUtlederService = brevUtlederService,
            brevbestillingService = brevbestillingService,
            behandlingRepository = InMemoryBehandlingRepository,
            trekkKlageService = trekkKlageService,
            avklaringsbehovService = avklaringsbehovService,
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository
        )

        // Runde-1

        every { brevbestillingService.harBestillingOmVedtak(any()) } returns false

        val resultat1 = steg.utfør(kontekst)

        assertThat(resultat1).isEqualTo(Fullført)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.SKRIV_VEDTAKSBREV)
        assertThat(avklaringsbehov!!.historikk).hasSize(1)
        assertThat(avklaringsbehov.historikk.get(0).status).isEqualTo(Status.OPPRETTET)

        verify(exactly = 1) { brevbestillingService.bestillV2(allAny(), allAny(), allAny(), allAny()) }

        // Runde-2

        // Saksbehandler avklarer vedtaksbrev og status på SKRIV_VEDTAKSBREV avklaringsbehov settes til AVSLUTTET
        InMemoryAvklaringsbehovRepository.endre(
            avklaringsbehovId = avklaringsbehov.id,
            endring = Endring(
                status = Status.AVSLUTTET,
                endretAv = "SAKSBEHANDLER",
                begrunnelse = "Brev ferdig"
            )
        )
        // vedtak løst av saksbehandler - da skal harBestillingOmVedtak() returnere true i BrevSteg utfør()
        every { brevbestillingService.harBestillingOmVedtak(any()) } returns true
        every { brevbestillingService.erAlleBestillingerOmVedtakIEndeTilstand(any()) } returns true

        val resultat2 = steg.utfør(kontekst)

        assertThat(resultat2).isEqualTo(Fullført)
        val avklaringsbehovene2 = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val avklaringsbehov2 = avklaringsbehovene2.hentBehovForDefinisjon(Definisjon.SKRIV_VEDTAKSBREV)
        assertThat(avklaringsbehov2!!.historikk).hasSize(2)
        assertThat(avklaringsbehov2.historikk.last().status).isEqualTo(Status.AVSLUTTET)
        // brevBestilling har ikke kjørt i runde-2 da det allerede er bestilt
        verify(exactly = 1) { brevbestillingService.bestillV2(allAny(), allAny(), allAny(), allAny()) }
    }

    @Test
    fun `BrevSteg uten behov for melding om vedtakbrev skal kun fullføre uten endring i avklaringsbehov og brevbestilling`() {
        val behandling = InMemoryBehandlingRepository.opprettBehandling(
            sakId = SakId(1L),
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(
                    VurderingsbehovMedPeriode(
                        Vurderingsbehov.MOTTATT_SØKNAD,
                        Periode(LocalDate.now().minusDays(1), LocalDate.now().plusYears(1))
                    )
                ),
                årsak = ÅrsakTilOpprettelse.SØKNAD,
                opprettet = LocalDateTime.now(),
                beskrivelse = "unit-test"
            )
        )
        val kontekst = FlytKontekstMedPerioder(
            sakId = behandling.sakId,
            behandlingId = behandling.id,
            behandlingType = behandling.typeBehandling(),
            forrigeBehandlingId = behandling.forrigeBehandlingId,
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            rettighetsperiode = Periode(LocalDate.now().minusDays(1), LocalDate.now().plusYears(1)),
            vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTTATT_SØKNAD)
        )
        val steg = MeldingOmVedtakBrevSteg(
            brevUtlederService = brevUtlederService,
            brevbestillingService = brevbestillingService,
            behandlingRepository = InMemoryBehandlingRepository,
            trekkKlageService = trekkKlageService,
            avklaringsbehovService = avklaringsbehovService,
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository
        )
        every { brevUtlederService.utledBehovForMeldingOmVedtak(any()) } returns null

        val resultat1 = steg.utfør(kontekst)

        assertThat(resultat1).isEqualTo(Fullført)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        assertThat(avklaringsbehovene.alle()).isEmpty()
        verify(exactly = 0) { brevbestillingService.bestillV2(allAny(), allAny(), allAny(), allAny()) }
    }

    @Test
    fun `BrevSteg hvor klage er trukket skal kun fullføre uten endring i avklaringsbehov og brevbestilling`() {
        val behandling = InMemoryBehandlingRepository.opprettBehandling(
            sakId = SakId(1L),
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(
                    VurderingsbehovMedPeriode(
                        Vurderingsbehov.MOTTATT_SØKNAD,
                        Periode(LocalDate.now().minusDays(1), LocalDate.now().plusYears(1))
                    )
                ),
                årsak = ÅrsakTilOpprettelse.SØKNAD,
                opprettet = LocalDateTime.now(),
                beskrivelse = "unit-test"
            )
        )
        val kontekst = FlytKontekstMedPerioder(
            sakId = behandling.sakId,
            behandlingId = behandling.id,
            behandlingType = behandling.typeBehandling(),
            forrigeBehandlingId = behandling.forrigeBehandlingId,
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            rettighetsperiode = Periode(LocalDate.now().minusDays(1), LocalDate.now().plusYears(1)),
            vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTTATT_SØKNAD)
        )
        val steg = MeldingOmVedtakBrevSteg(
            brevUtlederService = brevUtlederService,
            brevbestillingService = brevbestillingService,
            behandlingRepository = InMemoryBehandlingRepository,
            trekkKlageService = trekkKlageService,
            avklaringsbehovService = avklaringsbehovService,
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository
        )
        every { trekkKlageService.klageErTrukket(any()) } returns true

        val resultat1 = steg.utfør(kontekst)

        assertThat(resultat1).isEqualTo(Fullført)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        assertThat(avklaringsbehovene.alle()).isEmpty()
        verify(exactly = 0) { brevbestillingService.bestillV2(allAny(), allAny(), allAny(), allAny()) }
    }

}
