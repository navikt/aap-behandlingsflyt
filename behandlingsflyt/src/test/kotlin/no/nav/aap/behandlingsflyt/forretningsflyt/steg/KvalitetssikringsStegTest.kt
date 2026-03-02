package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
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
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
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

    private class Scenario {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
        private val behandling = opprettBehandling(periode)
        private val avklaringsbehovene = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)

        private val steg = KvalitetssikringsSteg(
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
            avklaringsbehovService = AvklaringsbehovService(
                inMemoryRepositoryProvider
            ),
            tidligereVurderinger = FakeTidligereVurderinger(),
            trekkKlageService = TrekkKlageService(inMemoryRepositoryProvider),
            unleashGateway = AlleAvskruddUnleash,
        )

        fun kjørSteg() {
            val kontekst = flytKontekstMedPerioder { behandling = this@Scenario.behandling }
            steg.utfør(kontekst)
        }

        private fun opprettBehandling(periode: Periode): Behandling {
            val person =
                Person(
                    PersonId(Random.nextLong()),
                    UUID.randomUUID(),
                    listOf(genererIdent(LocalDate.now().minusYears(23)))
                )
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
                    kontekst = FlytKontekst(
                        sakId = behandling.sakId,
                        behandlingId = behandling.id,
                        forrigeBehandlingId = null,
                        behandlingType = behandling.typeBehandling(),
                    )
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
