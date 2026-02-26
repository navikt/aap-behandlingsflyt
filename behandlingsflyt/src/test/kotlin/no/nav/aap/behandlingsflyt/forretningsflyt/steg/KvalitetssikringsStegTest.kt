package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.KvalitetssikrerLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.KvalitetssikringLøsning
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.LokalUnleash
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.random.Random

class KvalitetssikringsStegTest {

    private val steg = KvalitetssikringsSteg(
        avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
        avklaringsbehovService = AvklaringsbehovService(
            inMemoryRepositoryProvider
        ),
        tidligereVurderinger = FakeTidligereVurderinger(),
        trekkKlageService = TrekkKlageService(inMemoryRepositoryProvider),
        unleashGateway = LokalUnleash,
    )

    @Test
    fun `om sykdom er løst, så kreves kvalitetssikring`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
        val behandling = opprettBehandling(periode)

        val avklaringsbehovene = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)

        opprettOgLøs(avklaringsbehovene, periode, Definisjon.AVKLAR_SYKDOM)


        val kontekst = flytKontekstMedPerioder { this.behandling = behandling }

        steg.utfør(kontekst)

        // Kvalitetssikre
        kvalitetssikre(behandling, avklaringsbehovene)

        assertThat(
            avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SYKDOM)?.status()
        ).isEqualTo(Status.KVALITETSSIKRET)

        steg.utfør(kontekst)

        val behov2 = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.KVALITETSSIKRING)
        assertThat(behov2?.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `om sykdom løses og bistand, men kun sykdom kvalitetssikres, skal behovet holdes åpent`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
        val behandling = opprettBehandling(periode)

        val avklaringsbehovene = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)

        opprettOgLøs(avklaringsbehovene, periode, Definisjon.AVKLAR_SYKDOM)
        opprettOgLøs(avklaringsbehovene, periode, Definisjon.AVKLAR_BISTANDSBEHOV)


        val kontekst = flytKontekstMedPerioder { this.behandling = behandling }

        steg.utfør(kontekst)

        // Kvalitetssikre
        kvalitetssikre(behandling, avklaringsbehovene)

        assertThat(
            avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SYKDOM)?.status()
        ).isEqualTo(Status.KVALITETSSIKRET)

        assertThat(
            avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_BISTANDSBEHOV)?.status()
        ).isEqualTo(Status.AVSLUTTET)

        steg.utfør(kontekst)

        val behov2 = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.KVALITETSSIKRING)
        assertThat(behov2?.status()).isEqualTo(Status.OPPRETTET)
    }

    private fun kvalitetssikre(
        behandling: Behandling,
        avklaringsbehovene: Avklaringsbehovene
    ) {
        val resultat = KvalitetssikrerLøser(InMemoryAvklaringsbehovRepository, LokalUnleash).løs(
            AvklaringsbehovKontekst(
                bruker = Bruker("KVALIGUY"),
                kontekst = FlytKontekst(
                    sakId = behandling.sakId, behandlingId = behandling.id,
                    forrigeBehandlingId = null,
                    behandlingType = behandling.typeBehandling(),
                )
            ),
            KvalitetssikringLøsning(
                vurderinger = listOf(
                    TotrinnsVurdering(
                        definisjon = Definisjon.AVKLAR_SYKDOM.kode,
                        godkjent = true,
                        begrunnelse = null,
                        grunner = emptyList()
                    )
                )
            )
        )
        avklaringsbehovene.løsAvklaringsbehov(
            Definisjon.KVALITETSSIKRING,
            resultat.begrunnelse,
            "KVALIGUY",
            resultat.kreverToTrinn
        )
    }

    private fun opprettOgLøs(
        avklaringsbehovene: Avklaringsbehovene,
        periode: Periode,
        definisjon: Definisjon
    ) {
        avklaringsbehovene.leggTil(
            definisjon,
            funnetISteg = definisjon.løsesISteg,
            begrunnelse = "faf",
            bruker = Bruker("VEILEDER"),
            perioderSomIkkeErTilstrekkeligVurdert = setOf(periode),
            perioderVedtaketBehøverVurdering = setOf(periode)
        )
        avklaringsbehovene.løsAvklaringsbehov(definisjon, "fff", "fff")

        assertThat(avklaringsbehovene.hentBehovForDefinisjon(definisjon)?.erÅpent()).isFalse
    }

    private fun opprettBehandling(periode: Periode): Behandling {
        val person =
            Person(PersonId(Random.nextLong()), UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))
        val sak = InMemorySakRepository.finnEllerOpprett(person, periode)
        val behandling = InMemoryBehandlingRepository.opprettBehandling(
            sakId = sak.id,
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD
            )
        )
        return behandling
    }
}