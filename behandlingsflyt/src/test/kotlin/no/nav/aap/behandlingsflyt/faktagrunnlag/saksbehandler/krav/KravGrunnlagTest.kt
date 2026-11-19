package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class KravGrunnlagTest {
    private val behandlingId = BehandlingId(1L)

    @Test
    fun `gjeldende relevante krav inneholder både søknadskrav og migrert krav`() {
        val søknadKrav = søknadKrav(1 januar 2026)
        val migrertKrav = migrertKrav(2 mars 2026)
        val tilleggsopplysning = tilleggsopplysning()

        val grunnlag = KravGrunnlag(setOf(søknadKrav, migrertKrav, tilleggsopplysning))

        assertThat(grunnlag.gjeldendeRelevanteKrav()).containsExactlyInAnyOrder(søknadKrav, migrertKrav)
        assertThat(grunnlag.gjeldendeSøknadKrav()).containsExactly(søknadKrav)
        assertThat(grunnlag.gjeldendeMigrertKrav()).isEqualTo(migrertKrav)
    }

    @Test
    fun `kravtidslinjen inneholder migrert krav fra muligRettFra`() {
        val migrertKrav = migrertKrav(2 mars 2026)

        val segmenter = KravGrunnlag(setOf(migrertKrav)).kravtidslinje().segmenter()

        assertThat(segmenter).hasSize(1)
        assertThat(segmenter.single().periode.fom).isEqualTo(2 mars 2026)
        assertThat(segmenter.single().verdi).isEqualTo(migrertKrav)
    }

    @Test
    fun `migrert krav nedgradert til tilleggsopplysning er ikke lenger relevant`() {
        val migrertKrav = migrertKrav(2 mars 2026, opprettet = Instant.now().minusSeconds(60))
        val nedgradert = tilleggsopplysning(referanse = migrertKrav.referanse)

        val grunnlag = KravGrunnlag(setOf(migrertKrav, nedgradert))

        assertThat(grunnlag.gjeldendeRelevanteKrav()).isEmpty()
        assertThat(grunnlag.gjeldendeMigrertKrav()).isNull()
    }

    private fun søknadKrav(muligRettFra: LocalDate) = RelevantSøknadKrav(
        referanse = Kravreferanse.ny(),
        journalpostId = JournalpostId("jp-$muligRettFra"),
        vurdertAv = Bruker("Z123456"),
        begrunnelse = "Test",
        vurdertIBehandling = behandlingId,
        opprettet = Instant.now(),
        søknadsdato = Søknadsdato(muligRettFra, SøknadsdatoÅrsak.SøknadMottatt, begrunnelse = "Test"),
        overstyrMuligRettFra = null,
        muligRettFra = muligRettFra,
    )

    private fun migrertKrav(muligRettFra: LocalDate, opprettet: Instant = Instant.now()) = RelevantMigrertKrav(
        referanse = Kravreferanse.ny(),
        vurdertAv = Bruker("Z123456"),
        begrunnelse = "Test",
        vurdertIBehandling = behandlingId,
        opprettet = opprettet,
        virkningstidspunktArena = muligRettFra.minusYears(1),
        muligRettFra = muligRettFra,
        arenaSaksnummer = "ARENA-1",
        rettighetstype = MigrertRettighetstype.ORDINÆR,
        resterendeKvoteOrdinær = 100,
    )

    private fun tilleggsopplysning(referanse: Kravreferanse = Kravreferanse.ny()) = Tilleggsopplysning(
        referanse = referanse,
        journalpostId = JournalpostId("jp-tillegg"),
        vurdertAv = Bruker("Z123456"),
        begrunnelse = "Test",
        vurdertIBehandling = behandlingId,
        opprettet = Instant.now(),
    )
}
