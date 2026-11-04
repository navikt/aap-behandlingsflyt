package no.nav.aap.behandlingsflyt.behandling.egenskaper

import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkResponse
import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertRettighetstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.FakeArenaOppslagGateway
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryKravRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class BehandlingEgenskaperServiceTest {

    private val grunnlagService = BehandlingEgenskaperGrunnlagService(InMemoryKravRepository, InMemorySakRepository)
    private val service = lagService(harArenaHistorikk = false)

    @Test
    fun `behandling uten kravgrunnlag er ikke migrert fra Arena`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()

        assertThat(hentEgenskaper(behandling).erMigrertFraArena).isFalse()
    }

    @Test
    fun `behandling med kun vanlige krav er ikke migrert fra Arena`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        InMemoryKravRepository.lagre(behandling.id, setOf(lagRelevantKrav(behandling.id, Kravreferanse.ny())))

        assertThat(hentEgenskaper(behandling).erMigrertFraArena).isFalse()
    }

    @Test
    fun `behandling med gjeldende migrert krav er migrert fra Arena`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        InMemoryKravRepository.lagre(behandling.id, setOf(lagMigrertKrav(behandling.id, Kravreferanse.ny())))

        assertThat(hentEgenskaper(behandling).erMigrertFraArena).isTrue()
    }

    @Test
    fun `behandling der migrert krav er erstattet av nyere krav er ikke migrert fra Arena`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val referanse = Kravreferanse.ny()
        val migrertKrav = lagMigrertKrav(behandling.id, referanse, opprettet = Instant.parse("2025-01-01T00:00:00Z"))
        val nyereKrav = lagRelevantKrav(behandling.id, referanse, opprettet = Instant.parse("2025-02-01T00:00:00Z"))
        InMemoryKravRepository.lagre(behandling.id, setOf(migrertKrav, nyereKrav))

        assertThat(hentEgenskaper(behandling).erMigrertFraArena).isFalse()
    }

    @Test
    fun `harArenaHistorikk følger svaret fra Arena`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()

        assertThat(hentEgenskaper(behandling, lagService(harArenaHistorikk = true)).harArenaHistorikk).isTrue()
        assertThat(hentEgenskaper(behandling, lagService(harArenaHistorikk = false)).harArenaHistorikk).isFalse()
    }

    private fun hentEgenskaper(behandling: Behandling, service: BehandlingEgenskaperService = this.service) =
        service.hentEgenskaper(grunnlagService.hentGrunnlag(behandling))

    private fun lagService(harArenaHistorikk: Boolean) = BehandlingEgenskaperService(
        arenaOppslagGateway = object : FakeArenaOppslagGateway() {
            override fun hentHarHistorikk(ident: Ident) = HarHistorikkResponse(harHistorikk = harArenaHistorikk)
        },
    )

    private fun lagRelevantKrav(
        behandlingId: BehandlingId,
        referanse: Kravreferanse,
        opprettet: Instant = Instant.now(),
    ) = RelevantKrav(
        referanse = referanse,
        journalpostId = JournalpostId("123"),
        vurdertAv = SYSTEMBRUKER,
        begrunnelse = "Test",
        vurdertIBehandling = behandlingId,
        opprettet = opprettet,
        søknadsdato = Søknadsdato(MULIG_RETT_FRA, SøknadsdatoÅrsak.SøknadMottatt, begrunnelse = "Test"),
        overstyrMuligRettFra = null,
        muligRettFra = MULIG_RETT_FRA,
    )

    private fun lagMigrertKrav(
        behandlingId: BehandlingId,
        referanse: Kravreferanse,
        opprettet: Instant = Instant.now(),
    ) = MigrertKrav(
        referanse = referanse,
        vurdertAv = SYSTEMBRUKER,
        begrunnelse = "Migrert fra Arena",
        vurdertIBehandling = behandlingId,
        opprettet = opprettet,
        virkningstidspunktArena = MULIG_RETT_FRA,
        muligRettFra = MULIG_RETT_FRA,
        arenaSaksnummer = "ARENA-4711",
        rettighetstype = MigrertRettighetstype.ORDINÆR,
        resterendeKvoteOrdinær = 0,
    )

    private companion object {
        val MULIG_RETT_FRA: LocalDate = LocalDate.of(2024, 1, 1)
    }
}
