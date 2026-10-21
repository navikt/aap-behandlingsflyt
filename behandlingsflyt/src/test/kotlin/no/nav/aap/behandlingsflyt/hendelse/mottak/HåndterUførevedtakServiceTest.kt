package no.nav.aap.behandlingsflyt.hendelse.mottak

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.underveis.RettighetstypeService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.UføreSøknadVedtakResultat
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.UførevedtakReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakKilde
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakResultat
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakV0
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Tid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class HåndterUførevedtakServiceTest {
    private val dato = LocalDate.now()
    private val sakId = SakId(1)
    private val behandlingId = BehandlingId(2)
    private val behandling = mockk<Behandling>()
    private val behandlingService = mockk<BehandlingService>()
    private val trukketSøknadService = mockk<TrukketSøknadService>()
    private val rettighetstypeService = mockk<RettighetstypeService>()
    private val overgangUføreRepository = mockk<OvergangUføreRepository>()
    private val mottaDokumentService = mockk<MottaDokumentService>(relaxed = true)
    private val unleashGateway = mockk<UnleashGateway>()
    private val service = HåndterUførevedtakService(
        behandlingService, trukketSøknadService, rettighetstypeService, mottaDokumentService,
        mockk<ProsesserBehandlingService>(relaxed = true), mockk(), unleashGateway, overgangUføreRepository,
    )
    private val vedtak = UførevedtakV0(
        personId = "12345678901",
        virkningsdato = dato.minusDays(10),
        resultat = UførevedtakResultat.INNV,
        avslag12_5 = false,
        kilde = UførevedtakKilde.PENSJON,
    )
    private val referanse = InnsendingReferanse(UførevedtakReferanse.ny("test"))

    @BeforeEach
    fun setup() {
        every { behandling.id } returns behandlingId
        every { behandling.forrigeBehandlingId } returns null
        every { behandling.status() } returns Status.AVSLUTTET
        every { behandlingService.finnSisteYtelsesbehandlingFor(sakId) } returns behandling
        every { behandlingService.finnEllerOpprettBehandling(sakId, any()) } returns BehandlingService.Ordinær(behandling)
        every { trukketSøknadService.søknadErTrukket(behandlingId) } returns false
        every { overgangUføreRepository.hentHvisEksisterer(behandlingId) } returns null
        every { unleashGateway.isEnabled(BehandlingsflytFeature.AutomatiskOpphorUforeTilbakevirkende) } returns true
        rettighet(RettighetsType.VURDERES_FOR_UFØRETRYGD)
    }

    @Test
    fun `gjeldende automatisk opphør hindrer ny behandling ved gjentatt melding`() {
        vurderinger(vurdering(dato.minusDays(5)))
        håndter()
        verify(exactly = 0) { behandlingService.finnEllerOpprettBehandling(sakId, any()) }
        verify { mottaDokumentService.markerSomBehandlet(sakId, behandlingId, referanse) }
    }

    @Test
    fun `gjeldende manuelt opphør hindrer også ny behandling`() {
        vurderinger(vurdering(dato.minusDays(5)).copy(vurdertAv = Bruker("Z123456")))
        håndter()
        verify(exactly = 0) { behandlingService.finnEllerOpprettBehandling(sakId, any()) }
    }

    @Test
    fun `senere manuell vurdering overstyrer gammelt automatisk opphør`() {
        vurderinger(
            vurdering(dato.minusDays(5)),
            vurdering(dato.minusDays(2)).copy(
                vurdertIBehandling = BehandlingId(3),
                vurdertAv = Bruker("Z123456"),
                opprettet = Instant.now().plusSeconds(1),
                brukerRettPåAAP = true,
                brukerHarFåttVedtakOmUføretrygd = UføreSøknadVedtakResultat.NEI,
            ),
        )
        håndter()
        forventAutomatiskOpphør()
    }

    @Test
    fun `gammelt opphør blokkerer ikke ny uføreperiode`() {
        vurderinger(vurdering(dato.minusDays(20)))
        håndter()
        forventAutomatiskOpphør()
    }

    @Test
    fun `tidsbegrenset opphør blokkerer ikke ny vurdering`() {
        vurderinger(vurdering(dato.minusDays(5)).copy(tom = dato))
        håndter()
        forventAutomatiskOpphør()
    }

    @Test
    fun `bare historisk rett etter 11-18 gir ikke automatisk opphør`() {
        rettighet(RettighetsType.VURDERES_FOR_UFØRETRYGD, tom = dato)
        håndter()
        verify(exactly = 0) {
            behandlingService.finnEllerOpprettBehandling(sakId, match {
                it.vurderingsbehov.any { behov -> behov.type == Vurderingsbehov.OVERGANG_UFORE_AUTOMATISK_STANS }
            })
        }

    }

    @Test
    fun `tilbakevirkende uførevedtak håndteres manuelt når toggle er av`() {
        every { unleashGateway.isEnabled(BehandlingsflytFeature.AutomatiskOpphorUforeTilbakevirkende) } returns false
        håndter()
        verify(exactly = 1) {
            behandlingService.finnEllerOpprettBehandling(sakId, match {
                it.vurderingsbehov.single().type == Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
            })
        }
    }

    @Test
    fun `fremtidig uførevedtak håndteres automatisk selv om toggle er av`() {
        every { unleashGateway.isEnabled(BehandlingsflytFeature.AutomatiskOpphorUforeTilbakevirkende) } returns false
        service.håndterMottattUførevedtakHendelse(
            sakId, referanse, vedtak.copy(virkningsdato = dato.plusDays(10)), dato.atTime(12, 0)
        )
        forventAutomatiskOpphør()
    }

    @Test
    fun `uførevedtak med virkningsdato på mottaksdagen håndteres manuelt når toggle er av`() {
        every { unleashGateway.isEnabled(BehandlingsflytFeature.AutomatiskOpphorUforeTilbakevirkende) } returns false
        service.håndterMottattUførevedtakHendelse(
            sakId, referanse, vedtak.copy(virkningsdato = dato), dato.atTime(12, 0)
        )
        verify(exactly = 1) {
            behandlingService.finnEllerOpprettBehandling(sakId, match {
                it.vurderingsbehov.single().type == Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
            })
        }
    }

    private fun håndter() =
        service.håndterMottattUførevedtakHendelse(sakId, referanse, vedtak, dato.atTime(12, 0))

    private fun forventAutomatiskOpphør() {
        verify(exactly = 1) {
            behandlingService.finnEllerOpprettBehandling(sakId, match {
                it.vurderingsbehov.single().type == Vurderingsbehov.OVERGANG_UFORE_AUTOMATISK_STANS
            })
        }
    }

    private fun rettighet(type: RettighetsType, tom: LocalDate = Tid.MAKS) {
        every { rettighetstypeService.rettighetstypeTidslinjeBakoverkompatibel(behandlingId) } returns
                listOf(type).somTidslinje { Periode(dato.minusDays(30), tom) }
    }

    private fun vurderinger(vararg vurderinger: OvergangUføreVurdering) {
        every { overgangUføreRepository.hentHvisEksisterer(behandlingId) } returns
                OvergangUføreGrunnlag(vurderinger.toList())
    }

    private fun vurdering(fom: LocalDate) = OvergangUføreVurdering(
        begrunnelse = "Opphør",
        brukerHarSøktOmUføretrygd = true,
        brukerHarFåttVedtakOmUføretrygd = UføreSøknadVedtakResultat.JA_INNVILGET_FULL,
        brukerRettPåAAP = false,
        fom = fom,
        tom = null,
        vurdertAv = SYSTEMBRUKER,
        vurdertIBehandling = behandlingId,
        opprettet = Instant.now(),
    )
}
