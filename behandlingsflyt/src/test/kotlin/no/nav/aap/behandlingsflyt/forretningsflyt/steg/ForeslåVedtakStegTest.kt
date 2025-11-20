package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class ForeslåVedtakStegTest {

    private val random = Random(1235123)

    private val avklaringsbehovRepository = InMemoryAvklaringsbehovRepository
    private val avklaringsbehovService = AvklaringsbehovService(avklaringsbehovRepository, mockk {
        every { revurderingErAvbrutt(any()) } returns false
    })
    private val steg = ForeslåVedtakSteg(avklaringsbehovRepository, FakeTidligereVurderinger(), avklaringsbehovService)
    private val sakRepository = InMemorySakRepository


    private val behandlingRepository = InMemoryBehandlingRepository

    @Test
    fun `hvis ingen avklaringsbehov skal det ikke gi foreslå vedtak`() {
        val person =
            Person(PersonId(random.nextLong()), UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))

        val sak = sakRepository.finnEllerOpprett(person, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        val behandling =
            behandlingRepository.opprettBehandling(
                sakId = sak.id,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                    årsak = ÅrsakTilOpprettelse.SØKNAD
                )
            )
        val kontekstMedPerioder = FlytKontekstMedPerioder(
            sak.id, behandling.id, behandling.forrigeBehandlingId, behandling.typeBehandling(),
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTTATT_SØKNAD),
            rettighetsperiode = Periode(LocalDate.now(), LocalDate.now())
        )

        val resultat = steg.utfør(kontekstMedPerioder)

        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `hvis ingen avklaringsbehov løst av NAY skal foreslå vedtak hoppes over`() {
        val person =
            Person(PersonId(random.nextLong()), UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))

        val sak = sakRepository.finnEllerOpprett(person, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        val behandling =
            behandlingRepository.opprettBehandling(
                sakId = sak.id,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                    årsak = ÅrsakTilOpprettelse.SØKNAD
                )
            )
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
        avklaringsbehovene.leggTil(
            definisjoner = listOf(Definisjon.AVKLAR_SYKDOM),
            funnetISteg = StegType.AVKLAR_SYKDOM
        )
        avklaringsbehovene.løsAvklaringsbehov(Definisjon.AVKLAR_SYKDOM, "ja", "TESTEN")
        val kontekstMedPerioder = FlytKontekstMedPerioder(
            sak.id, behandling.id, behandling.forrigeBehandlingId, behandling.typeBehandling(),
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTTATT_SØKNAD),
            rettighetsperiode = Periode(LocalDate.now(), LocalDate.now())
        )

        val resultat = steg.utfør(kontekstMedPerioder)

        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `hvis avklaringsbehov løst av NAY, gå innom foreslå vedtak`() {
        val person =
            Person(PersonId(random.nextLong()), UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))

        val sak = sakRepository.finnEllerOpprett(person, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        val behandling =
            behandlingRepository.opprettBehandling(
                sakId = sak.id,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                    årsak = ÅrsakTilOpprettelse.SØKNAD
                )
            )
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
        avklaringsbehovene.leggTil(
            definisjoner = listOf(Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP),
            funnetISteg = StegType.VURDER_LOVVALG
        )
        avklaringsbehovene.løsAvklaringsbehov(Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP, "ja", "TESTEN")
        val kontekstMedPerioder = FlytKontekstMedPerioder(
            sak.id, behandling.id, behandling.forrigeBehandlingId, behandling.typeBehandling(),
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTTATT_SØKNAD),
            rettighetsperiode = Periode(LocalDate.now(), LocalDate.now())
        )

        val resultat = steg.utfør(kontekstMedPerioder)

        assertThat(resultat).isEqualTo(Fullført)
        assertThat(avklaringsbehovene.åpne().map { it.definisjon }).containsExactly(Definisjon.FORESLÅ_VEDTAK)
    }

    @Test
    fun `hvis NAY-avklaringsbehov skal foreslå vedtak åpnes også etter tilbakehopp`() {
        val person =
            Person(PersonId(random.nextLong()), UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))

        val sak = sakRepository.finnEllerOpprett(person, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        val behandling =
            behandlingRepository.opprettBehandling(
                sakId = sak.id,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                    årsak = ÅrsakTilOpprettelse.SØKNAD
                )
            )
        var avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
        avklaringsbehovene.leggTil(
            definisjoner = listOf(Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP),
            funnetISteg = StegType.VURDER_LOVVALG
        )
        avklaringsbehovene.løsAvklaringsbehov(Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP, "ja", "TESTEN")
        avklaringsbehovene.leggTil(
            definisjoner = listOf(Definisjon.FORESLÅ_VEDTAK),
            funnetISteg = StegType.FORESLÅ_VEDTAK
        )
        
        avklaringsbehovene.løsAvklaringsbehov(Definisjon.FORESLÅ_VEDTAK, "ja", "TESTEN")
        val kontekstMedPerioder = FlytKontekstMedPerioder(
            sak.id, behandling.id, behandling.forrigeBehandlingId, behandling.typeBehandling(),
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTTATT_SØKNAD),
            rettighetsperiode = Periode(LocalDate.now(), LocalDate.now())
        )

        val resultatFørTilbakehopp = steg.utfør(kontekstMedPerioder)
        assertThat(resultatFørTilbakehopp).isEqualTo(Fullført)
        avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
        assertThat(avklaringsbehovene.åpne()).isEmpty()

        // Gjør om på et NAY-avklaringsbehov
        avklaringsbehovene.løsAvklaringsbehov(Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP, "gjorde om på noe", "TESTEN")
        
        val resultatEtterTilbakehopp = steg.utfør(kontekstMedPerioder)
        assertThat(resultatEtterTilbakehopp).isEqualTo(Fullført)
        avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
        assertThat(avklaringsbehovene.åpne().map { it.definisjon }).containsExactly(Definisjon.FORESLÅ_VEDTAK)
    }
}