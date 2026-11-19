package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.ArenaMeldeperiodesyklusInformasjonskrav.IngenSyklus
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.periodisering.FlytKontekstMedPeriodeService
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.test.FakeArenaOppslagGateway
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryInformasjonskravRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMeldeperiodeRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.minimalGatewayProvider
import no.nav.aap.behandlingsflyt.test.september
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
        val fastsattDag = kjørInformasjonskrav(behandling)
        assertThat(fastsattDag).isNull()
    }

    @Test
    fun `endrer ikke fastsatt dag hvis fastsatt dag allerede er satt men mangler i Arena`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        meldekortsyklus = IngenSyklus
        InMemoryMeldeperiodeRepository.lagreFastsattDag(behandling.id, 13 februar 2017)
        val fastsattDag = kjørInformasjonskrav(behandling)
        assertThat(fastsattDag).isEqualTo(13 februar 2017)
    }

    @Test
    fun `endrer ikke fastsatt dag hvis fastsatt dag allerede er satt selv om den finnes i Arena`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        meldekortsyklus = ArenaMeldeperiodesyklusInformasjonskrav.HarSyklus(år = 2025, ukenummer = 40)
        InMemoryMeldeperiodeRepository.lagreFastsattDag(behandling.id, 13 februar 2017)
        val fastsattDag = kjørInformasjonskrav(behandling)
        assertThat(fastsattDag).isEqualTo(13 februar 2017)
    }

    @Test
    fun `setter fastsatt dag hvis fastsatt dag ikke er satt men finnes i Arena`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        meldekortsyklus = ArenaMeldeperiodesyklusInformasjonskrav.HarSyklus(år = 2026, ukenummer = 40)
        val fastsattDag = kjørInformasjonskrav(behandling)
        assertThat(fastsattDag).isEqualTo(28 september 2026)
    }

    @Test
    fun `setter fastsatt dag riktig hvis første uke`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        meldekortsyklus = ArenaMeldeperiodesyklusInformasjonskrav.HarSyklus(år = 2026, ukenummer = 1)
        val fastsattDag = kjørInformasjonskrav(behandling)
        assertThat(fastsattDag).isEqualTo(29 desember 2025)
    }

    @Test
    fun `setter fastsatt dag riktig hvis siste uke er uke 53`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        meldekortsyklus = ArenaMeldeperiodesyklusInformasjonskrav.HarSyklus(år = 2026, ukenummer = 53)
        val fastsattDag = kjørInformasjonskrav(behandling)
        assertThat(fastsattDag).isEqualTo(28 desember 2026)
    }

    private fun kjørInformasjonskrav(behandling: Behandling): LocalDate? {
        val flytKontekstMedPeridoeService = FlytKontekstMedPeriodeService(inMemoryRepositoryProvider, minimalGatewayProvider {  })
        val kontekst = flytKontekstMedPeridoeService.utled(behandling.flytKontekst(), StegType.FASTSETT_MELDEPERIODER)
        arenaMeldeperiodesyklusInformasjonskrav.run {
            if (erRelevant(kontekst, StegType.FASTSETT_MELDEPERIODER, null)) {
                val input = klargjør(kontekst)
                val data = hentData(input)
                oppdater(input, data, kontekst)
            }
        }
        return InMemoryMeldeperiodeRepository.hentFastsattDag(behandling.id)
    }
}