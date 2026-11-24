package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.ArenaMeldeperiodesyklusInformasjonskrav.HarSyklus
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.ArenaMeldeperiodesyklusInformasjonskrav.IngenSyklus
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.periodisering.FlytKontekstMedPeriodeService
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.test.FakeArenaOppslagGateway
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.august
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.behandlingsflyt.test.oktober
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMeldeperiodeRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.minimalGatewayProvider
import no.nav.aap.behandlingsflyt.test.september
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

class ArenaMeldeperiodesyklusInformasjonskravTest {
    var meldekortsyklus: ArenaMeldeperiodesyklusInformasjonskrav.Registerdata = IngenSyklus

    private val arenaOppslagGateway = object : FakeArenaOppslagGateway() {
        override fun hentArenaMeldekortsyklus(ident: Ident): ArenaMeldeperiodesyklusInformasjonskrav.Registerdata {
            return meldekortsyklus
        }
    }

    private val arenaMeldeperiodesyklusInformasjonskrav = ArenaMeldeperiodesyklusInformasjonskrav(
        sakRepository = InMemorySakRepository,
        meldeperiodeRepository = InMemoryMeldeperiodeRepository,
        unleashGateway = object :
            FakeUnleashBaseWithDefaultDisabled(listOf(BehandlingsflytFeature.SynkroniserArenaMeldeperiodesyklus)) {},
        arenaOppslagGateway = arenaOppslagGateway,
    )

    @Test
    fun `setter ikke fastsatt dag hvis ingen syklus i Arena`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        meldekortsyklus = IngenSyklus
        val (fastsattDag, endret) = kjørInformasjonskrav(behandling)
        assertThat(fastsattDag).isNull()
        assertThat(endret).isEqualTo(Informasjonskrav.Endret.IKKE_ENDRET)
    }

    @Test
    fun `endrer ikke fastsatt dag hvis fastsatt dag allerede er satt men mangler i Arena`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        meldekortsyklus = IngenSyklus
        InMemoryMeldeperiodeRepository.lagreFastsattDag(behandling.id, 13 februar 2017)
        val (fastsattDag, _) = kjørInformasjonskrav(behandling)
        assertThat(fastsattDag).isEqualTo(13 februar 2017)
    }

    @Test
    fun `endrer ikke fastsatt dag hvis fastsatt dag allerede er satt selv om den finnes i Arena`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 1 oktober 2025)
        meldekortsyklus = HarSyklus(år = 2025, ukenummer = 40)
        InMemoryMeldeperiodeRepository.lagreFastsattDag(behandling.id, 13 februar 2017)
        val (fastsattDag, _) = kjørInformasjonskrav(behandling)
        assertThat(fastsattDag).isEqualTo(13 februar 2017)
    }

    @Test
    fun `setter fastsatt dag hvis fastsatt dag ikke er satt men finnes i Arena`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 1 oktober 2026)
        meldekortsyklus = HarSyklus(år = 2026, ukenummer = 40)
        val (fastsattDag, endret) = kjørInformasjonskrav(behandling)
        assertThat(fastsattDag).isEqualTo(28 september 2026)
        assertThat(endret).isEqualTo(Informasjonskrav.Endret.ENDRET)
    }

    @Test
    fun `setter fastsatt dag riktig hvis første uke`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 5 januar 2026)
        meldekortsyklus = HarSyklus(år = 2026, ukenummer = 1)
        val (fastsattDag, _) = kjørInformasjonskrav(behandling)
        assertThat(fastsattDag).isEqualTo(29 desember 2025)
    }

    @Test
    fun `setter fastsatt dag riktig hvis siste uke er uke 53`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 4 januar 2027)
        meldekortsyklus = HarSyklus(år = 2026, ukenummer = 53)
        val (fastsattDag, _) = kjørInformasjonskrav(behandling)
        assertThat(fastsattDag).isEqualTo(28 desember 2026)
    }

    @Test
    fun `setter fastsatt dag når startuke er 6 uker før rettighetsperiodens start`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 28 september 2026)
        meldekortsyklus = HarSyklus(år = 2026, ukenummer = 34)
        val (fastsattDag, endret) = kjørInformasjonskrav(behandling)
        assertThat(fastsattDag).isEqualTo(17 august 2026)
        assertThat(endret).isEqualTo(Informasjonskrav.Endret.ENDRET)
    }

    @Test
    fun `setter ikke fastsatt dag når startuke er mer enn 6 uker før rettighetsperiodens start`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 28 september 2026)
        meldekortsyklus = HarSyklus(år = 2026, ukenummer = 33)
        val (fastsattDag, endret) = kjørInformasjonskrav(behandling)
        assertThat(fastsattDag).isNull()
        assertThat(endret).isEqualTo(Informasjonskrav.Endret.IKKE_ENDRET)
    }

    @Test
    fun `synkroniserer over årsskiftet når startuke er uke 53 og rettighetsperioden starter i uke 2`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 13 januar 2027)
        meldekortsyklus = HarSyklus(år = 2026, ukenummer = 53)
        val (fastsattDag, endret) = kjørInformasjonskrav(behandling)
        assertThat(fastsattDag).isEqualTo(28 desember 2026)
        assertThat(endret).isEqualTo(Informasjonskrav.Endret.ENDRET)
    }

    @Test
    fun `synkroniserer ikke når rettighetsperioden starter i uke 10 og startuke er uke 1`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 8 mars 2027)
        meldekortsyklus = HarSyklus(år = 2027, ukenummer = 1)
        val (fastsattDag, endret) = kjørInformasjonskrav(behandling)
        assertThat(fastsattDag).isNull()
        assertThat(endret).isEqualTo(Informasjonskrav.Endret.IKKE_ENDRET)
    }

    @ParameterizedTest(name = "startuke {0}-W{1}, referanse {2} → {3}")
    @CsvSource(
        // samme uke
        "2026, 40, 2026-09-28, true",
        "2026, 40, 2026-10-04, true",
        // grensen på 6 uker
        "2026, 34, 2026-09-28, true",
        "2026, 33, 2026-09-28, false",
        "2026, 33, 2026-10-04, false",
        // uke 10 mot uke 1
        "2027, 1, 2027-03-08, false",
        // årsskifte med uke 53
        "2026, 53, 2027-01-11, true",
        "2026, 49, 2027-01-11, true",
        "2026, 48, 2027-01-11, false",
        // årsskifte uten uke 53
        "2025, 52, 2026-01-05, true",
        "2025, 48, 2026-01-05, true",
        "2025, 47, 2026-01-05, false",
        // startuke etter referanseuka
        "2026, 42, 2026-09-28, true",
    )
    fun `skalSynkronisere`(år: Int, uke: Long, referansedato: LocalDate, forventet: Boolean) {
        val startdato = HarSyklus(år = år, ukenummer = uke).startdato
        assertThat(ArenaMeldeperiodesyklusInformasjonskrav.skalSynkronisere(startdato, referansedato))
            .isEqualTo(forventet)
    }

    @Test
    fun `startdato er mandag i ISO-uka`() {
        assertThat(HarSyklus(år = 2026, ukenummer = 1).startdato).isEqualTo(29 desember 2025)
        assertThat(HarSyklus(år = 2026, ukenummer = 53).startdato).isEqualTo(28 desember 2026)
        assertThat(HarSyklus(år = 2027, ukenummer = 1).startdato).isEqualTo(4 januar 2027)
    }

    private fun kjørInformasjonskrav(behandling: Behandling): Pair<LocalDate?, Informasjonskrav.Endret?> {
        val flytKontekstMedPeridoeService = FlytKontekstMedPeriodeService(inMemoryRepositoryProvider, minimalGatewayProvider {  })
        val kontekst = flytKontekstMedPeridoeService.utled(behandling.flytKontekst(), StegType.FASTSETT_MELDEPERIODER)
        val endret = arenaMeldeperiodesyklusInformasjonskrav.run {
            if (erRelevant(kontekst, StegType.FASTSETT_MELDEPERIODER, null)) {
                val input = klargjør(kontekst)
                val data = hentData(input)
                oppdater(input, data, kontekst)
            } else {
                null
            }
        }
        return InMemoryMeldeperiodeRepository.hentFastsattDag(behandling.id) to endret
    }
}
