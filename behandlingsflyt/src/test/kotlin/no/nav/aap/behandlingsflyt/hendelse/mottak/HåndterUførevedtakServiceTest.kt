package no.nav.aap.behandlingsflyt.hendelse.mottak

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.underveis.RettighetstypeService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status.BEHANDLET
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.UføreSøknadVedtakResultat
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.UførevedtakReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakKilde
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakResultat
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakV1
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryFlytJobbRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMottattDokumentRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryOvergangUføreRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTrukketSøknadRepository
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

class HåndterUførevedtakServiceTest {
    private val dato = LocalDate.now()
    private val sakId = SakId(1)
    private val behandlingId = BehandlingId(idSekvens.incrementAndGet())
    private val behandling = Behandling(
        id = behandlingId,
        forrigeBehandlingId = null,
        sakId = sakId,
        typeBehandling = TypeBehandling.Revurdering,
        status = Status.AVSLUTTET,
        årsakTilOpprettelse = null,
        versjon = 1,
    )
    private val behandlingService = mockk<BehandlingService>()
    private val trukketSøknadService = TrukketSøknadService(InMemoryTrukketSøknadRepository)
    private val rettighetstypeService = mockk<RettighetstypeService>()
    private val overgangUføreRepository = InMemoryOvergangUføreRepository
    private val mottaDokumentService = MottaDokumentService(InMemoryMottattDokumentRepository)
    private val service = HåndterUførevedtakService(
        behandlingService = behandlingService,
        trukketSøknadService = trukketSøknadService,
        rettighetstypeService = rettighetstypeService,
        mottaDokumentService = mottaDokumentService,
        prosesserBehandlingService = mockk<ProsesserBehandlingService>(relaxed = true),
        flytJobbRepository = InMemoryFlytJobbRepository,
        overgangUføreRepository = overgangUføreRepository,
    )
    private val vedtak = UførevedtakV0(
        personId = "12345678901",
        virkningsdato = dato.minusDays(10),
        resultat = UførevedtakResultat.INNV,
        avslag12_5 = false,
        kilde = UførevedtakKilde.PENSJON,
    )
    private val referanse = InnsendingReferanse(UførevedtakReferanse.ny("test-${behandlingId.id}"))

    @BeforeEach
    fun setup() {
        every { behandlingService.finnSisteGjeldendeEllerÅpneYtelsesbehandling(sakId) } returns behandling
        every { behandlingService.finnEllerOpprettBehandling(sakId, any()) } returns BehandlingService.Ordinær(behandling)
        InMemoryMottattDokumentRepository.lagre(
            MottattDokument(
                referanse = referanse,
                sakId = sakId,
                behandlingId = behandlingId,
                mottattTidspunkt = dato.atTime(12, 0),
                type = InnsendingType.UFØRE_VEDTAK_HENDELSE,
                kanal = Kanal.DIGITAL,
                strukturertDokument = StrukturertDokument(vedtak),
            )
        )
        rettighet(RettighetsType.VURDERES_FOR_UFØRETRYGD)
    }

    @AfterEach
    fun ryddOpp() {
        overgangUføreRepository.slett(behandlingId)
        InMemoryMottattDokumentRepository.slett(behandlingId)
    }

    @Test
    fun `gjeldende automatisk opphør hindrer ny behandling ved gjentatt melding`() {
        vurderinger(vurdering(dato.minusDays(5)))
        håndter()
        verify(exactly = 0) { behandlingService.finnEllerOpprettBehandling(sakId, any()) }
        assertThat(InMemoryMottattDokumentRepository.hent(sakId, referanse).status)
            .isEqualTo(BEHANDLET)
    }

    @Test
    fun `gjeldende manuelt opphør hindrer også ny behandling`() {
        vurderinger(vurdering(dato.minusDays(5)).copy(vurdertAv = Bruker("Z123456")))
        håndter()
        verify(exactly = 0) { behandlingService.finnEllerOpprettBehandling(sakId, any()) }
    }

    @Test
    fun `opphør mellom virkningsdato og vedtaksdato hindrer ny behandling`() {
        vurderinger(vurdering(dato.minusDays(7)))
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
    fun `rettighet som slutter før vedtaksdato gir ikke automatisk opphør`() {
        rettighet(
            RettighetsType.VURDERES_FOR_UFØRETRYGD,
            vedtak.virkningsdato,
            dato.minusDays(6),
        )
        håndter()
        verify(exactly = 1) {
            behandlingService.finnEllerOpprettBehandling(sakId, match {
                it.vurderingsbehov.single().type == Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
            })
        }
    }

    @Test
    fun `manglende vedtaksdato gir manuell behandling`() {
        service.håndterMottattUførevedtakHendelse(sakId, referanse, vedtak, dato.atTime(12, 0))
        verify(exactly = 1) {
            behandlingService.finnEllerOpprettBehandling(sakId, match {
                it.vurderingsbehov.single().type == Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
            })
        }
    }

    @Test
    fun `automatisk opphør sjekker rettighet fra uførevedtakets vedtaksdato`() {
        val vedtaksdato = dato.minusDays(5)
        rettighet(RettighetsType.VURDERES_FOR_UFØRETRYGD, vedtaksdato, vedtaksdato)
        service.håndterMottattUførevedtakHendelse(
            sakId,
            referanse,
            UførevedtakV1(
                personId = vedtak.personId,
                virkningsdato = vedtak.virkningsdato,
                resultat = vedtak.resultat,
                avslag12_5 = vedtak.avslag12_5,
                kilde = vedtak.kilde,
                vedtaksdato = vedtaksdato
            ),
            dato.atTime(12, 0)
        )
        forventAutomatiskOpphør()
    }

    @Test
    fun `opphør på vedtaksdato hindrer ny behandling selv om virkningsdato er senere`() {
        val vedtaksdato = vedtak.virkningsdato.minusDays(1)
        vurderinger(vurdering(vedtaksdato))
        service.håndterMottattUførevedtakHendelse(
            sakId,
            referanse,
            UførevedtakV1(
                vedtak.personId, vedtak.virkningsdato, vedtak.resultat, vedtak.avslag12_5, vedtak.kilde, vedtaksdato
            ),
            dato.atTime(12, 0)
        )
        verify(exactly = 0) { behandlingService.finnEllerOpprettBehandling(sakId, any()) }
    }

    @Test
    fun `fremtidig uførevedtak håndteres automatisk`() {
        service.håndterMottattUførevedtakHendelse(
            sakId, referanse, vedtak.copy(virkningsdato = dato.plusDays(10)), dato.atTime(12, 0)
        )
        forventAutomatiskOpphør()
    }

    @Test
    fun `uførevedtak med vedtaksdato og virkningsdato på mottaksdagen håndteres automatisk`() {
        service.håndterMottattUførevedtakHendelse(
            sakId, referanse,
            UførevedtakV1(
                vedtak.personId, dato, vedtak.resultat, vedtak.avslag12_5, vedtak.kilde, dato.minusDays(1)
            ),
            dato.atTime(12, 0)
        )
        forventAutomatiskOpphør()
    }

    @Test
    fun `uførevedtak med virkningsdato på mottaksdagen håndteres automatisk`() {
        service.håndterMottattUførevedtakHendelse(
            sakId, referanse, vedtak.copy(virkningsdato = dato), dato.atTime(12, 0)
        )
        forventAutomatiskOpphør()
    }

    private fun håndter() =
        service.håndterMottattUførevedtakHendelse(
            sakId, referanse,
            UførevedtakV1(
                vedtak.personId, vedtak.virkningsdato, vedtak.resultat, vedtak.avslag12_5, vedtak.kilde,
                dato.minusDays(5)
            ),
            dato.atTime(12, 0)
        )

    private fun forventAutomatiskOpphør() {
        verify(exactly = 1) {
            behandlingService.finnEllerOpprettBehandling(sakId, match {
                it.vurderingsbehov.single().type == Vurderingsbehov.OVERGANG_UFORE_AUTOMATISK_STANS
            })
        }
    }

    private fun rettighet(
        type: RettighetsType,
        fom: LocalDate = dato.minusDays(30),
        tom: LocalDate = Tid.MAKS,
    ) {
        every { rettighetstypeService.rettighetstypeTidslinjeBakoverkompatibel(behandlingId) } returns
                listOf(type).somTidslinje { Periode(fom, tom) }
    }

    private fun vurderinger(vararg vurderinger: OvergangUføreVurdering) {
        overgangUføreRepository.lagre(behandlingId, vurderinger.toList())
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

    companion object {
        private val idSekvens = AtomicLong(100000)
    }
}
