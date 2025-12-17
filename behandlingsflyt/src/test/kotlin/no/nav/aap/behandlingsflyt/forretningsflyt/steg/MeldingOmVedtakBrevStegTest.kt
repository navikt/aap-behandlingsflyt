package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Endring
import no.nav.aap.behandlingsflyt.behandling.brev.BrevUtlederService
import no.nav.aap.behandlingsflyt.behandling.brev.SignaturService
import no.nav.aap.behandlingsflyt.behandling.brev.VedtakAktivitetsplikt11_7
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingGateway
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBrevbestillingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random

class MeldingOmVedtakBrevStegTest {

    private lateinit var brevbestillingService: BrevbestillingService
    private val signaturService = mockk<SignaturService>()
    private val brevbestillingGateway = mockk<BrevbestillingGateway>()
    private val brevUtlederService = mockk<BrevUtlederService>()
    private val trekkKlageService = mockk<TrekkKlageService>()
    private val avklaringsbehovService = AvklaringsbehovService(inMemoryRepositoryProvider)
    private lateinit var avklaringsbehovRepository: AvklaringsbehovRepository
    private val behandlingId = BehandlingId(Random.nextLong())

    @BeforeEach
    fun setup() {
        every { trekkKlageService.klageErTrukket(any()) } returns false
        every { brevUtlederService.utledBehovForMeldingOmVedtak(any()) } returns VedtakAktivitetsplikt11_7
        every { brevbestillingGateway.slett(any()) } returns Unit
        every { signaturService.finnSignaturGrunnlag(any(), any()) } returns emptyList()
        every { brevbestillingGateway.ferdigstill(any(), any()) } returns true
        avklaringsbehovRepository = InMemoryAvklaringsbehovRepository
        brevbestillingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository = InMemoryBehandlingRepository,
            sakRepository = InMemorySakRepository
        )
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
     *       - manuell avklaring resulterer i ny status=AVSLUTTET for avklaringsbehov SKRIV_VEDTAKSBREV
     *       - oppdaterAvklaringsbehov() i BrevSteg etter manuell avklaring ivaretar status AVSLUTTET, da erTilstrekkeligVurdert er true
     *       - BrevSteg utfører ikke bestillBrev()
     */
    @Test
    fun `Runder i BrevSteg ved førstegangsbehandling resulterer i opprettet og avsluttet avklaringsbehov samt brevbestilling`() {
        val (kontekst, steg) = opprettDataTilSteg()

        // Runde-1

        val brevbestillingReferanse = BrevbestillingReferanse(UUID.randomUUID())
        every { brevbestillingGateway.bestillBrev(any(), any(), any(), any(), any(), any(), any(), any()) } returns brevbestillingReferanse

        val resultat1 = steg.utfør(kontekst)

        assertThat(resultat1).isEqualTo(Fullført)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.SKRIV_VEDTAKSBREV)
        assertThat(avklaringsbehov!!.historikk).hasSize(1)
        assertThat(avklaringsbehov.historikk.get(0).status).isEqualTo(Status.OPPRETTET)
        verify(exactly = 1) { brevbestillingGateway.bestillBrev(any(), any(), any(), any(), any(), any(), any(), any()) }

        // Runde-2
        // Saksbehandler avklarer vedtaksbrev og status på SKRIV_VEDTAKSBREV avklaringsbehov settes til AVSLUTTET
        // vedtak løst av saksbehandler og ferdigstilt mot aap-brev
        // Alternativ: bruk løs() i SkrivVedtaksbrevLøser for mer komplette testomgivelser
        InMemoryAvklaringsbehovRepository.endre(
            avklaringsbehovId = avklaringsbehov.id,
            endring = Endring(
                status = Status.AVSLUTTET,
                endretAv = "SAKSBEHANDLER",
                begrunnelse = "Brev ferdig"
            )
        )
        brevbestillingService.ferdigstill(behandlingId, brevbestillingReferanse, Bruker("SAKSBEHANDLER"))

        val resultat2 = steg.utfør(kontekst)

        assertThat(resultat2).isEqualTo(Fullført)
        val avklaringsbehovene2 = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val avklaringsbehov2 = avklaringsbehovene2.hentBehovForDefinisjon(Definisjon.SKRIV_VEDTAKSBREV)
        assertThat(avklaringsbehov2!!.historikk.size > 1)
        assertThat(avklaringsbehov2.historikk.last().status).isEqualTo(Status.AVSLUTTET)
        // brevBestilling har ikke kjørt i runde-2 da det allerede er bestilt
        verify(exactly = 1) { brevbestillingGateway.bestillBrev(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `BrevSteg uten behov for melding om vedtakbrev skal kun fullføre uten endring i avklaringsbehov og brevbestilling`() {
        val (kontekst, steg) = opprettDataTilSteg()

        every { brevUtlederService.utledBehovForMeldingOmVedtak(any()) } returns null

        val resultat1 = steg.utfør(kontekst)

        assertThat(resultat1).isEqualTo(Fullført)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        assertThat(avklaringsbehovene.alle()).isEmpty()
        verify(exactly = 0) { brevbestillingGateway.bestillBrev(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `BrevSteg hvor klage er trukket skal kun fullføre uten endring i avklaringsbehov og brevbestilling`() {
        val (kontekst, steg) = opprettDataTilSteg()

        every { trekkKlageService.klageErTrukket(any()) } returns true

        val resultat1 = steg.utfør(kontekst)

        assertThat(resultat1).isEqualTo(Fullført)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        assertThat(avklaringsbehovene.alle()).isEmpty()
        verify(exactly = 0) { brevbestillingGateway.bestillBrev(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `Teoretisk tilbakestill i BrevSteg ved førstegangsbehandling resulterer i opprettet, avbrutt og avsluttet avklaringsbehov samt brevbestilling`() {
        val (kontekst, steg) = opprettDataTilSteg()

        val brevbestillingReferanse = BrevbestillingReferanse(UUID.randomUUID())
        every { brevbestillingGateway.bestillBrev(any(), any(), any(), any(), any(), any(), any(), any()) } returns brevbestillingReferanse

        // Runde-1

        val resultat1 = steg.utfør(kontekst)

        assertThat(resultat1).isEqualTo(Fullført)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.SKRIV_VEDTAKSBREV)
        assertThat(avklaringsbehov!!.historikk).hasSize(1)
        assertThat(avklaringsbehov.historikk.get(0).status).isEqualTo(Status.OPPRETTET)
        verify(exactly = 1) { brevbestillingGateway.bestillBrev(any(), any(), any(), any(), any(), any(), any(), any()) }
        val bestilling = InMemoryBrevbestillingRepository.hent(brevbestillingReferanse)
        assertThat(bestilling.status).isEqualTo(no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FORHÅNDSVISNING_KLAR)

        // Tilbakestill-runde-1.5

        // Trigg tilbakestill
        every { brevUtlederService.utledBehovForMeldingOmVedtak(any()) } returns null

        val resultatTilbakestill = steg.utfør(kontekst)

        assertThat(resultatTilbakestill).isEqualTo(Fullført)
        val avklaringsbehoveneTilbakestillt = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val avklaringsbehovTilbakestilt = avklaringsbehoveneTilbakestillt.hentBehovForDefinisjon(Definisjon.SKRIV_VEDTAKSBREV)
        assertThat(avklaringsbehovTilbakestilt!!.historikk.size > 1)
        assertThat(avklaringsbehovTilbakestilt.historikk.last().status).isEqualTo(Status.AVBRUTT)
        // brevBestilling kall har ikke økt i tilbakestill-runde-1.5, men nytt kall til tilbakestill er nå utført
        verify(exactly = 1) { brevbestillingGateway.bestillBrev(any(), any(), any(), any(), any(), any(), any(), any()) }
        verify(exactly = 1) { brevbestillingGateway.slett(any()) }
        val tilbakestiltBestilling = InMemoryBrevbestillingRepository.hent(brevbestillingReferanse)
        assertThat(tilbakestiltBestilling.status).isEqualTo(no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.TILBAKESTILT)

        // Runde-2

        // Saksbehandler avklarer vedtaksbrev og status på SKRIV_VEDTAKSBREV avklaringsbehov settes til AVSLUTTET
        // TODO: Men kan saksbehandler vurdere et tilbakestilt vedtakbrev ?? Frontend må ta hensyn til ny status TILBAKESTILT ??
        InMemoryAvklaringsbehovRepository.endre(
            avklaringsbehovId = avklaringsbehov.id,
            endring = Endring(
                status = Status.AVSLUTTET,
                endretAv = "SAKSBEHANDLER",
                begrunnelse = "Brev ferdig"
            )
        )
        val brevbestillingReferanse2 = BrevbestillingReferanse(UUID.randomUUID())
        every { brevbestillingGateway.bestillBrev(any(), any(), any(), any(), any(), any(), any(), any()) } returns brevbestillingReferanse2
        every { brevUtlederService.utledBehovForMeldingOmVedtak(any()) } returns VedtakAktivitetsplikt11_7

        val resultat2 = steg.utfør(kontekst)

        // TODO: Hvorfor får vi OPPRETTET avklaringsbehov etter denne runden i BrevSteg ??!! -- har det noe med ny sletting av brevbestilling ved tilbakestill ??
        assertThat(resultat2).isEqualTo(Fullført)
        val avklaringsbehovene2 = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val avklaringsbehov2 = avklaringsbehovene2.hentBehovForDefinisjon(Definisjon.SKRIV_VEDTAKSBREV)
        assertThat(avklaringsbehov2!!.historikk.last().status).isEqualTo(Status.OPPRETTET)
        // brevBestilling har kjørt i runde-2 da forrige bestilling er tilbakestilt
        verify(exactly = 2) { brevbestillingGateway.bestillBrev(any(), any(), any(), any(), any(), any(), any(), any()) }
        val bestilling2 = InMemoryBrevbestillingRepository.hent(brevbestillingReferanse2)
        assertThat(bestilling2.status).isEqualTo(no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FORHÅNDSVISNING_KLAR)
    }

    private fun opprettDataTilSteg(): Pair<FlytKontekstMedPerioder, MeldingOmVedtakBrevSteg> {
        val sak = InMemorySakRepository.finnEllerOpprett(
            person = Person(
                PersonId(Random.nextLong()),
                UUID.randomUUID(),
                listOf(Ident("12345678911"))
            ),
            periode = Periode(fom = LocalDate.now(), tom = LocalDate.now().plusYears(1))
        )
        val behandling = InMemoryBehandlingRepository.opprettBehandling(
            sakId = sak.id,
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
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
            unleashGateway = FakeUnleash,
        )
        return Pair(kontekst, steg)
    }

}
