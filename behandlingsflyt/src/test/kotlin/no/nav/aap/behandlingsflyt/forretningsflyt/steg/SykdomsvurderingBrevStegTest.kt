package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySykdomsvurderingForBrevRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class SykdomsvurderingBrevStegTest {

    private val random = Random(1235123)

    private val steg = SykdomsvurderingBrevSteg(
        InMemorySykdomsvurderingForBrevRepository,
        AvklaringsbehovService(inMemoryRepositoryProvider),
        InMemoryAvklaringsbehovRepository,
        InMemoryBehandlingRepository,
        FakeTidligereVurderinger()
    )
    private val sakRepository = InMemorySakRepository
    private val behandlingRepository = InMemoryBehandlingRepository

    @Test
    fun `skal opprette avklaringsbehov dersom ingen vurdering allerede finnes`() {
        val person = person()
        val sak = sak(person)
        val behandling = behandling(sak, typeBehandling = TypeBehandling.Førstegangsbehandling)
        val kontekstMedPerioder = flytKontekstMedPerioder(sak, behandling, VurderingType.FØRSTEGANGSBEHANDLING)

        opprettOgLøsSykdombehov(behandling)

        steg.utfør(kontekstMedPerioder)

        val behov = hentBrevBehov(behandling)

        assertThat(behov).isNotNull
    }

    @Test
    fun `skal returnere fullført dersom avklaringsbehov allerede finnes`() {
        val person = person()
        val sak = sak(person)
        val behandling = behandling(sak, typeBehandling = TypeBehandling.Førstegangsbehandling)

        InMemorySykdomsvurderingForBrevRepository.lagre(
            behandling.id, SykdomsvurderingForBrev(
                behandlingId = behandling.id,
                vurdering = "En vurdering",
                vurdertAv = "ident",
            )
        )

        val resultat = hentBrevBehov(behandling)

        assertThat(resultat).isNull()
    }

    @Test
    fun `skal opprette avklaringsbehov ved revurdering`() {
        val person = person()
        val sak = sak(person)
        val behandling = behandling(sak, typeBehandling = TypeBehandling.Revurdering)
        val kontekstMedPerioder = flytKontekstMedPerioder(
            sak,
            behandling,
            VurderingType.REVURDERING,
            setOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND)
        )

        opprettOgLøsSykdombehov(behandling)

        steg.utfør(kontekstMedPerioder)

        val behov = hentBrevBehov(behandling)

        assertThat(behov).isNotNull
    }

    private fun hentBrevBehov(behandling: Behandling): Avklaringsbehov? =
        InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
            .hentBehovForDefinisjon(Definisjon.SKRIV_SYKDOMSVURDERING_BREV)

    private fun opprettOgLøsSykdombehov(behandling: Behandling) {
        InMemoryAvklaringsbehovRepository.opprett(
            behandling.id,
            Definisjon.AVKLAR_SYKDOM,
            Definisjon.AVKLAR_SYKDOM.løsesISteg,
            null,
            "..."
        )
        InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
            .løsAvklaringsbehov(Definisjon.AVKLAR_SYKDOM, "...", "meg")
    }

    private fun flytKontekstMedPerioder(
        sak: Sak,
        behandling: Behandling,
        vurderingType: VurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
        vurderingsbehov: Set<Vurderingsbehov> = setOf(Vurderingsbehov.MOTTATT_SØKNAD)
    ): FlytKontekstMedPerioder = FlytKontekstMedPerioder(
        sak.id, behandling.id, behandling.forrigeBehandlingId, behandling.typeBehandling(),
        vurderingType = vurderingType,
        vurderingsbehovRelevanteForSteg = vurderingsbehov,
        rettighetsperiode = Periode(LocalDate.now(), LocalDate.now())
    )

    private fun behandling(sak: Sak, typeBehandling: TypeBehandling): Behandling =
        behandlingRepository.opprettBehandling(
            sakId = sak.id,
            typeBehandling = typeBehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD
            )
        )

    private fun sak(person: Person): Sak =
        sakRepository.finnEllerOpprett(person, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))

    private fun person(): Person =
        Person(PersonId(random.nextLong()), UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))
}