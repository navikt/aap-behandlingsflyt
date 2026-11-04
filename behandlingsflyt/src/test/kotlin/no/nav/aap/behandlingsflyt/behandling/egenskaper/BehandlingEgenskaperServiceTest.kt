package no.nav.aap.behandlingsflyt.behandling.egenskaper

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertRettighetstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryKravRepository
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

class BehandlingEgenskaperServiceTest {

    private val service = BehandlingEgenskaperService(InMemoryKravRepository)

    @Test
    fun `behandling uten kravgrunnlag er ikke migrert fra Arena`() {
        val behandlingId = nyBehandlingId()

        assertThat(service.hentEgenskaper(behandlingId).erMigrertFraArena).isFalse()
    }

    @Test
    fun `behandling med kun vanlige krav er ikke migrert fra Arena`() {
        val behandlingId = nyBehandlingId()
        InMemoryKravRepository.lagre(behandlingId, setOf(lagRelevantKrav(behandlingId, Kravreferanse.ny())))

        assertThat(service.hentEgenskaper(behandlingId).erMigrertFraArena).isFalse()
    }

    @Test
    fun `behandling med gjeldende migrert krav er migrert fra Arena`() {
        val behandlingId = nyBehandlingId()
        InMemoryKravRepository.lagre(behandlingId, setOf(lagMigrertKrav(behandlingId, Kravreferanse.ny())))

        assertThat(service.hentEgenskaper(behandlingId).erMigrertFraArena).isTrue()
    }

    @Test
    fun `behandling der migrert krav er erstattet av nyere krav er ikke migrert fra Arena`() {
        val behandlingId = nyBehandlingId()
        val referanse = Kravreferanse.ny()
        val migrertKrav = lagMigrertKrav(behandlingId, referanse, opprettet = Instant.parse("2025-01-01T00:00:00Z"))
        val nyereKrav = lagRelevantKrav(behandlingId, referanse, opprettet = Instant.parse("2025-02-01T00:00:00Z"))
        InMemoryKravRepository.lagre(behandlingId, setOf(migrertKrav, nyereKrav))

        assertThat(service.hentEgenskaper(behandlingId).erMigrertFraArena).isFalse()
    }

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
        private val nesteId = AtomicLong(900_000)
        fun nyBehandlingId() = BehandlingId(nesteId.incrementAndGet())
    }
}
