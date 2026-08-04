package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class ArbeidsopptrappingGrunnlagTest {
    @Test
    fun `hentPerioder filtrerer ut vurderinger som ikke har rettigheter`() {
        val grunnlag = ArbeidsopptrappingGrunnlag(
            listOf(
                ArbeidsopptrappingVurdering(
                    begrunnelse = "test1",
                    fom = LocalDate.of(2024, 1, 1),
                    tom = null,
                    reellMulighetTilOpptrapping = false,
                    rettPaaAAPIOpptrapping = true,
                    vurdertAv = Bruker("aa"),
                    opprettet = Instant.now(),
                    vurdertIBehandling = BehandlingId(1L)
                ),
                ArbeidsopptrappingVurdering(
                    begrunnelse = "test2",
                    fom = LocalDate.of(2024, 2, 1),
                    tom = null,
                    reellMulighetTilOpptrapping = true,
                    rettPaaAAPIOpptrapping = true,
                    vurdertAv = Bruker("bb"),
                    opprettet = Instant.now().plusNanos(123),
                    vurdertIBehandling = BehandlingId(1L)
                )
            )
        )

        val perioder = grunnlag.perioderMedArbeidsopptrapping()

        assertThat(perioder).hasSize(1)
        assertThat(perioder.first().fom).isEqualTo(LocalDate.of(2024, 2, 1))
        assertThat(perioder.first().tom).isEqualTo(LocalDate.of(2024, 2, 1).plusYears(1).minusDays(1))
    }

    @Test
    fun `hentPerioder bruker neste vurdering som tom-dato hvis vurderingenGjelderTil er null`() {
        val grunnlag = ArbeidsopptrappingGrunnlag(
            listOf(
                ArbeidsopptrappingVurdering(
                    begrunnelse = "test",
                    fom = LocalDate.of(2024, 1, 1),
                    tom = null,
                    reellMulighetTilOpptrapping = true,
                    rettPaaAAPIOpptrapping = true,
                    vurdertAv = Bruker("aa"),
                    opprettet = Instant.now(),
                    vurdertIBehandling = BehandlingId(1L)
                ),
                ArbeidsopptrappingVurdering(
                    begrunnelse = "test",
                    fom = LocalDate.of(2024, 3, 1),
                    tom = null,
                    reellMulighetTilOpptrapping = true,
                    rettPaaAAPIOpptrapping = true,
                    vurdertAv = Bruker("bb"),
                    opprettet = Instant.now(),
                    vurdertIBehandling = BehandlingId(2L)
                )
            )
        )

        val perioder = grunnlag.perioderMedArbeidsopptrapping()

        assertThat(perioder).containsExactly(
            Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)),
        )
    }

    @Test
    fun `hentPerioder gir siste vurdering 12 måneder varighet hvis ingen neste og ingen til-dato`() {
        val grunnlag = ArbeidsopptrappingGrunnlag(
            listOf(
                ArbeidsopptrappingVurdering(
                    begrunnelse = "test",
                    fom = LocalDate.of(2024, 6, 1),
                    tom = null,
                    reellMulighetTilOpptrapping = true,
                    rettPaaAAPIOpptrapping = true,
                    vurdertAv = Bruker("aa"),
                    opprettet = Instant.now(),
                    vurdertIBehandling = BehandlingId(1L)
                )
            )
        )

        val perioder = grunnlag.perioderMedArbeidsopptrapping()

        assertThat(perioder.single())
            .isEqualTo(Periode(LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31)))
    }
}