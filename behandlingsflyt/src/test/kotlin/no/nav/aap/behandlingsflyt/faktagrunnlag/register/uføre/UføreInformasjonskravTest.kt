package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBeregningVurderingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryPersonopplysningRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryUføreRepository
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UføreInformasjonskravTest {

    private val sakService = SakService(InMemorySakRepository, InMemoryBehandlingRepository)
    private val uføreRepository = InMemoryUføreRepository
    private val beregningVurderingRepository = InMemoryBeregningVurderingRepository
    private val uføreRegisterGateway = object : UføreRegisterGateway {
        override fun innhentMedHistorikk(
            person: Person,
            fraDato: LocalDate
        ): Set<Uføre> = TODO()

        override fun hentÅpenUføreSøknad(person: Person): UføreSøknad = TODO()
    }
    private val tidligereVurderinger = FakeTidligereVurderinger()

    private val informasjonskrav = UføreInformasjonskrav(
        sakService = sakService,
        uføreRepository = uføreRepository,
        behandlingRepository = InMemoryBehandlingRepository,
        beregningVurderingRepository = beregningVurderingRepository,
        personopplysningRepository = InMemoryPersonopplysningRepository,
        uføreRegisterGateway = uføreRegisterGateway,
        tidligereVurderinger = tidligereVurderinger,
    )

    @Test
    fun `skal være relevant for overgang uføre stans`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val kontekst = flytKontekstMedPerioder {
            this.behandling = behandling
        }

        val resultat = informasjonskrav.erRelevant(kontekst, StegType.OVERGANG_UFORE, null)

        assertThat(resultat).isTrue()
    }

    @Test
    fun `skal ikke flette uføredata fra en meldekortbehandling inn i åpen behandling`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling()
        val meldekortbehandling = opprettAtomærBehandling(
            sak.id,
            behandling.id,
            ÅrsakTilOpprettelse.MELDEKORT,
            Vurderingsbehov.MOTTATT_MELDEKORT
        )
        val nyUføredata = Uføre(virkningstidspunkt = LocalDate.of(2024, 1, 1), uføregrad = Prosent(60))
        uføreRepository.lagre(behandling.id, setOf(nyUføredata))
        uføreRepository.lagre(
            meldekortbehandling.id,
            setOf(Uføre(virkningstidspunkt = LocalDate.of(2024, 1, 1), uføregrad = Prosent(50)))
        )

        val kontekst = behandling.flytKontekst().copy(forrigeBehandlingId = meldekortbehandling.id)
        val resultat = informasjonskrav.flettOpplysningerFraAtomærBehandling(kontekst)

        assertThat(resultat).isEqualTo(Informasjonskrav.Endret.IKKE_ENDRET)
        assertThat(uføreRepository.hentHvisEksisterer(behandling.id)?.vedtak).isEqualTo(setOf(nyUføredata))
    }

    @Test
    fun `skal flette uføredata fra uførevedtakshendelse`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling()
        val uførebehandling = opprettAtomærBehandling(
            sak.id,
            behandling.id,
            ÅrsakTilOpprettelse.UFØRE_VEDTAK_HENDELSE,
            Vurderingsbehov.OVERGANG_UFORE_AUTOMATISK_STANS
        )
        val gammelUføredata = Uføre(virkningstidspunkt = LocalDate.of(2023, 1, 1), uføregrad = Prosent(50))
        val nyUføredata = Uføre(virkningstidspunkt = LocalDate.of(2024, 1, 1), uføregrad = Prosent(60))
        uføreRepository.lagre(behandling.id, setOf(gammelUføredata))
        uføreRepository.lagre(uførebehandling.id, setOf(nyUføredata))

        val kontekst = behandling.flytKontekst().copy(forrigeBehandlingId = uførebehandling.id)
        val resultat = informasjonskrav.flettOpplysningerFraAtomærBehandling(kontekst)

        assertThat(resultat).isEqualTo(Informasjonskrav.Endret.ENDRET)
        assertThat(uføreRepository.hentHvisEksisterer(behandling.id)?.vedtak)
            .isEqualTo(setOf(gammelUføredata, nyUføredata))
    }

    private fun opprettAtomærBehandling(
        sakId: SakId,
        forrigeBehandlingId: BehandlingId,
        årsak: ÅrsakTilOpprettelse,
        vurderingsbehov: Vurderingsbehov,
    ) = InMemoryBehandlingRepository.opprettBehandling(
        sakId = sakId,
        typeBehandling = TypeBehandling.Revurdering,
        forrigeBehandlingId = forrigeBehandlingId,
        vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
            vurderingsbehov = listOf(VurderingsbehovMedPeriode(vurderingsbehov)),
            årsak = årsak,
        ),
    )
}
