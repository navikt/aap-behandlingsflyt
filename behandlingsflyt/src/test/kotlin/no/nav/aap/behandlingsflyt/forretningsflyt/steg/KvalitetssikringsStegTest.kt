package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.KvalitetssikrerLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.KvalitetssikringLøsning
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.LokalUnleash
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.minimalGatewayProvider
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class KvalitetssikringsStegTest {

    @Test
    fun `om sykdom er løst, så kreves kvalitetssikring`() {
        Scenario().apply {
            opprettOgLøs(Definisjon.AVKLAR_SYKDOM)

            kjørSteg()

            kvalitetssikre(Definisjon.AVKLAR_SYKDOM)

            assertStatus(Definisjon.AVKLAR_SYKDOM, Status.KVALITETSSIKRET)

            kjørSteg()

            assertStatus(Definisjon.KVALITETSSIKRING, Status.AVSLUTTET)
        }
    }

    @Test
    fun `om sykdom løses og bistand, men kun sykdom kvalitetssikres, skal behovet holdes åpent`() {
        Scenario().apply {
            opprettOgLøs(Definisjon.AVKLAR_SYKDOM)
            opprettOgLøs(Definisjon.AVKLAR_BISTANDSBEHOV)

            kjørSteg()

            // Kvalitetssikrer kun sykdom
            kvalitetssikre(Definisjon.AVKLAR_SYKDOM)

            assertStatus(Definisjon.AVKLAR_SYKDOM, Status.KVALITETSSIKRET)
            assertStatus(Definisjon.AVKLAR_BISTANDSBEHOV, Status.AVSLUTTET)

            kjørSteg()
            // Behovet er fremdeles åpent
            assertStatus(Definisjon.KVALITETSSIKRING, Status.OPPRETTET)

            // Kvalitetssikrer bistand
            kvalitetssikre(Definisjon.AVKLAR_BISTANDSBEHOV)

            kjørSteg()

            // Nå er behovet løst
            assertStatus(Definisjon.KVALITETSSIKRING, Status.AVSLUTTET)
        }
    }

    @Test
    fun `om et behov underkjennes, og løses på nytt, så skal det kvalitetssikres på nytt`() {
        Scenario().apply {
            opprettOgLøs(Definisjon.AVKLAR_SYKDOM)
            opprettOgLøs(Definisjon.SKRIV_SYKDOMSVURDERING_BREV)

            kjørSteg()
            assertStatus(Definisjon.KVALITETSSIKRING, Status.OPPRETTET)

            kvalitetssikre(Definisjon.AVKLAR_SYKDOM, godkjent = false)
            assertStatus(Definisjon.AVKLAR_SYKDOM, Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER)

            opprettOgLøs(Definisjon.AVKLAR_SYKDOM)

            kvalitetssikre(Definisjon.AVKLAR_SYKDOM, godkjent = true)
            assertStatus(Definisjon.AVKLAR_SYKDOM, Status.KVALITETSSIKRET)
            assertStatus(Definisjon.KVALITETSSIKRING, Status.AVSLUTTET)
        }
    }

    @Test
    fun `om et behov godkjennes, og løses på nytt, så skal det kvalitetssikres på nytt`() {
        Scenario().apply {
            opprettOgLøs(Definisjon.AVKLAR_SYKDOM)

            kjørSteg()
            assertStatus(Definisjon.KVALITETSSIKRING, Status.OPPRETTET)

            kvalitetssikre(Definisjon.AVKLAR_SYKDOM, godkjent = true)
            assertStatus(Definisjon.AVKLAR_SYKDOM, Status.KVALITETSSIKRET)

            kjørSteg()
            assertStatus(Definisjon.KVALITETSSIKRING, Status.AVSLUTTET)

            opprettOgLøs(Definisjon.AVKLAR_SYKDOM)

            kjørSteg()
            assertStatus(Definisjon.AVKLAR_SYKDOM, Status.AVSLUTTET)
            assertStatus(Definisjon.KVALITETSSIKRING, Status.OPPRETTET)
        }
    }

    private class Scenario {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
        private val behandling = opprettInMemorySakOgBehandling(periode.fom).second
        private val avklaringsbehovene = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)

        private val steg = KvalitetssikringsSteg(
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
            avklaringsbehovService = AvklaringsbehovService(
                inMemoryRepositoryProvider
            ),
            tidligereVurderinger = FakeTidligereVurderinger(),
            trekkKlageService = TrekkKlageService(inMemoryRepositoryProvider),
            unleashGateway = FakeUnleashBaseWithDefaultDisabled(
                enabledFlags = listOf(BehandlingsflytFeature.AlleEndringerKreverKvalitetssikring)
            ),
            avbrytRevurderingService = AvbrytRevurderingService(inMemoryRepositoryProvider.provide()),
            resultatUtleder = ResultatUtleder(inMemoryRepositoryProvider, minimalGatewayProvider()),
            behandlingRepository = inMemoryRepositoryProvider.provide()
        )

        fun kjørSteg() {
            val kontekst = flytKontekstMedPerioder { behandling = this@Scenario.behandling }
            steg.utfør(kontekst)
        }

        fun opprettOgLøs(
            definisjon: Definisjon
        ) {
            avklaringsbehovene.leggTil(
                definisjon,
                funnetISteg = definisjon.løsesISteg,
                begrunnelse = "faf",
                bruker = Bruker(VEILEDER),
                perioderSomIkkeErTilstrekkeligVurdert = setOf(periode),
                perioderVedtaketBehøverVurdering = setOf(periode)
            )
            avklaringsbehovene.løsAvklaringsbehov(definisjon, "fff", VEILEDER)

            assertThat(avklaringsbehovene.hentBehovForDefinisjon(definisjon)?.erÅpent())
                .`as`("Avklaringsbehov $definisjon skal være lukket etter løsning")
                .isFalse
        }

        fun kvalitetssikre(godkjente: List<Definisjon>, underkjente: List<Definisjon> = emptyList()) {
            val løser = KvalitetssikrerLøser(InMemoryAvklaringsbehovRepository, LokalUnleash)
            val resultat = løser.løs(
                AvklaringsbehovKontekst(
                    bruker = Bruker(KVALITETSSIKRER),
                    kontekst = behandling.flytKontekst(),
                ),
                KvalitetssikringLøsning(
                    vurderinger = (godkjente + underkjente).map {
                        TotrinnsVurdering(
                            definisjon = it.kode,
                            godkjent = it in godkjente,
                            begrunnelse = if (it in underkjente) "Ikke godkjent" else null,
                            grunner = emptyList()
                        )
                    }
                )
            )

            avklaringsbehovene.løsAvklaringsbehov(
                Definisjon.KVALITETSSIKRING,
                resultat.begrunnelse,
                KVALITETSSIKRER,
                resultat.kreverToTrinn
            )
        }

        fun kvalitetssikre(definisjon: Definisjon, godkjent: Boolean = true) {
            kvalitetssikre(
                if (godkjent) listOf(definisjon) else emptyList(),
                if (!godkjent) listOf(definisjon) else emptyList()
            )
        }

        fun assertStatus(definisjon: Definisjon, expected: Status) {
            val actual = avklaringsbehovene.hentBehovForDefinisjon(definisjon)?.status()
            assertThat(actual)
                .`as`("Status på $definisjon")
                .isEqualTo(expected)
        }
    }

    private companion object {
        const val VEILEDER = "VEILEDER"
        const val KVALITETSSIKRER = "KVALIGUY"
    }
}
