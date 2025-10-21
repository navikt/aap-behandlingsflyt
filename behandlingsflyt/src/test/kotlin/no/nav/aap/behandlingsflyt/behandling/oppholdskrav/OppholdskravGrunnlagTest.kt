package no.nav.aap.behandlingsflyt.behandling.oppholdskrav

import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.behandlingsflyt.test.november
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class OppholdskravGrunnlagTest {

    @Test
    fun `tidslinje skal la nyeste vurderinger overstyre eldre`() {
        val grunnlag = OppholdskravGrunnlag(
            opprettet = LocalDateTime.now(),
            vurderinger = listOf(
                OppholdskravVurdering(
                    opprettet = LocalDateTime.now().minusDays(6),
                    vurdertAv = "test",
                    vurdertIBehandling = BehandlingId(1L),
                    perioder = listOf(
                        OppholdskravPeriode(
                            fom = 1 januar 2024,
                            tom = null,
                            oppfylt = true,
                            land = null,
                            begrunnelse = "test"
                        )
                    )
                ),
                OppholdskravVurdering(
                    opprettet = LocalDateTime.now().minusDays(1),
                    vurdertAv = "test",
                    vurdertIBehandling = BehandlingId(2L),
                    perioder = listOf(
                        OppholdskravPeriode(
                            fom = 2 februar  2024,
                            tom = 1 mars 2024,
                            oppfylt = false,
                            land = "Behamas",
                            begrunnelse = "Var på ferie"
                        ),
                        OppholdskravPeriode(
                            fom = 2 mars 2024,
                            tom = null,
                            oppfylt = true,
                            land = null,
                            begrunnelse = "Tilbake fra ferie"
                        )
                    )
                ),
            ),
        )

        val rettighetsperiode = Periode(fom = 1 januar 2024, tom = 31 desember 2024)
        val tidslinje = grunnlag.tidslinje().begrensetTil(rettighetsperiode)

        assertThat(tidslinje.perioder()).hasSize(3)
        assertTidslinje(tidslinje,
            Periode(fom = rettighetsperiode.fom, tom = 1 februar 2024) to {
                assertThat(it.oppfylt).isTrue
            },
            Periode(fom = 2 februar  2024, tom = 1 mars 2024) to {
                assertThat(it.oppfylt).isFalse
            },
            Periode(fom = 2 mars 2024, tom = rettighetsperiode.tom) to {
                assertThat(it.oppfylt).isTrue
            },
        )
    }

    @Test
    fun `Valideringen skal godta en veldig enkel happycase med 1 periode`() {
        val grunnlag = OppholdskravGrunnlag(
            opprettet = LocalDateTime.now(),
            vurderinger = listOf(
                OppholdskravVurdering(
                    opprettet = LocalDateTime.now().minusDays(1),
                    vurdertAv = "test",
                    vurdertIBehandling = BehandlingId(2L),
                    perioder = listOf(
                        OppholdskravPeriode(
                            fom = 1 januar  2024,
                            tom = null,
                            oppfylt = true,
                            land = null,
                            begrunnelse = "Var på ferie"
                        ),
                    )
                ),
            ),
        )

        val rettighetsperiode = Periode(fom = 1 januar 2024, tom = 31 desember 2024)
        val valideringResult = grunnlag.tidslinje().validerGyldigForRettighetsperiode(rettighetsperiode)

        assertThat(valideringResult.isValid).isTrue
    }

    @Test
    fun `Valideringen skal sjekke at tidslinjen dekker hele rettighetsperioden`() {
        val grunnlag = OppholdskravGrunnlag(
            opprettet = LocalDateTime.now(),
            vurderinger = listOf(
                OppholdskravVurdering(
                    opprettet = LocalDateTime.now().minusDays(1),
                    vurdertAv = "test",
                    vurdertIBehandling = BehandlingId(2L),
                    perioder = listOf(
                        OppholdskravPeriode(
                            fom = 1 januar  2024,
                            tom = 1 mars 2024,
                            oppfylt = false,
                            land = "Behamas",
                            begrunnelse = "Var på ferie"
                        ),
                        OppholdskravPeriode(
                            fom = 2 mars 2024,
                            tom = 29 november 2024,
                            oppfylt = true,
                            land = null,
                            begrunnelse = "Tilbake fra ferie"
                        )
                    )
                ),
            ),
        )

        val rettighetsperiode = Periode(fom = 1 januar 2024, tom = 31 desember 2024)
        val valideringResult = grunnlag.tidslinje().validerGyldigForRettighetsperiode(rettighetsperiode)

        assertThat(valideringResult.isInvalid).isTrue
    }

    @Test
    fun `Valideringen skal sjekke at tidslinjen ikke har hull`() {
        val grunnlag = OppholdskravGrunnlag(
            opprettet = LocalDateTime.now(),
            vurderinger = listOf(
                OppholdskravVurdering(
                    opprettet = LocalDateTime.now().minusDays(1),
                    vurdertAv = "test",
                    vurdertIBehandling = BehandlingId(2L),
                    perioder = listOf(
                        OppholdskravPeriode(
                            fom = 1 januar  2024,
                            tom = 1 mars 2024,
                            oppfylt = false,
                            land = "Behamas",
                            begrunnelse = "Var på ferie"
                        ),
                        OppholdskravPeriode(
                            fom = 20 mars 2024,
                            tom = null,
                            oppfylt = true,
                            land = null,
                            begrunnelse = "Tilbake fra ferie"
                        )
                    )
                ),
            ),
        )

        val rettighetsperiode = Periode(fom = 1 januar 2024, tom = 31 desember 2024)
        val valideringResult = grunnlag.tidslinje().validerGyldigForRettighetsperiode(rettighetsperiode)

        assertThat(valideringResult.isInvalid).isTrue
    }
}