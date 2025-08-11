package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySykdomsvurderingForBrevRepository
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class SykdomsvurderingBrevStegTest {

    private val random = Random(1235123)

    private val steg = SykdomsurderingBrevSteg(InMemorySykdomsvurderingForBrevRepository, FakeTidligereVurderinger())
    private val sakRepository = InMemorySakRepository
    private val behandlingRepository = InMemoryBehandlingRepository

    @Test
    fun `skal opprette avklaringsbehov dersom ingen vurdering allerede finnes`() {
        val person = person()
        val sak = sak(person)
        val behandling = behandling(sak, typeBehandling = TypeBehandling.Førstegangsbehandling)
        val kontekstMedPerioder = flytKontekstMedPerioder(sak, behandling, VurderingType.FØRSTEGANGSBEHANDLING)

        val resultat = steg.utfør(kontekstMedPerioder)

        assertThat(resultat).isEqualTo(FantAvklaringsbehov(avklaringsbehov = listOf(Definisjon.SKRIV_SYKDOMSVURDERING_BREV)))
    }

    @Test
    fun `skal returnere fullført dersom avklaringsbehov allerede finnes`() {
        val person = person()
        val sak = sak(person)
        val behandling = behandling(sak, typeBehandling = TypeBehandling.Førstegangsbehandling)
        val kontekstMedPerioder = flytKontekstMedPerioder(sak, behandling, VurderingType.FØRSTEGANGSBEHANDLING)


        InMemorySykdomsvurderingForBrevRepository.lagre(behandling.id, SykdomsvurderingForBrev(
            behandlingId = behandling.id,
            vurdering = "En vurdering",
            vurdertAv = "ident",
        ))

        val resultat = steg.utfør(kontekstMedPerioder)

        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `skal opprette avklaringsbehov ved revurdering`() {
        val person = person()
        val sak = sak(person)
        val behandling = behandling(sak, typeBehandling = TypeBehandling.Revurdering)
        val kontekstMedPerioder = flytKontekstMedPerioder(sak, behandling, VurderingType.REVURDERING, setOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND))

        val resultat = steg.utfør(kontekstMedPerioder)

        assertThat(resultat).isEqualTo(FantAvklaringsbehov(avklaringsbehov = listOf(Definisjon.SKRIV_SYKDOMSVURDERING_BREV)))
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
        behandlingRepository.opprettBehandling(sak.id, listOf(), typeBehandling, null)

    private fun sak(person: Person): Sak =
        sakRepository.finnEllerOpprett(person, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))

    private fun person(): Person =
        Person(PersonId(random.nextLong()), UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))
}