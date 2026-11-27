package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.GjenståendeSykedager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.GjenståendeSykedagerGrunnlag
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.januar
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class GjenståendeSykedagerTest {
    @ParameterizedTest
    @EnumSource(
        GjenståendeSykedagerGrunnlag::class,
        names = ["VEDTAK_I_NY_LØSNING", "SISTE_UTBETALING_I_INFOTRYGD", "MAKSDATO_I_INFOTRYGD"]
    )
    fun `utleder maksdato ved å legge gjenstående hverdager til vurdertTilOgMed`(
        grunnlag: GjenståendeSykedagerGrunnlag
    ) {
        val sykedager = GjenståendeSykedager(grunnlag, 5, 1 januar 2026)

        assertThat(sykedager.utledMaksdato()).isEqualTo(8 januar 2026)
    }

    @ParameterizedTest
    @EnumSource(
        GjenståendeSykedagerGrunnlag::class,
        names = ["VEDTAK_I_NY_LØSNING", "SISTE_UTBETALING_I_INFOTRYGD", "MAKSDATO_I_INFOTRYGD"]
    )
    fun `teller første hverdag etter vurdertTilOgMed når datoen er i en helg`(
        grunnlag: GjenståendeSykedagerGrunnlag
    ) {
        for (vurdertTilOgMed in listOf(3 januar 2026, 4 januar 2026)) {
            val sykedager = GjenståendeSykedager(grunnlag, 1, vurdertTilOgMed)

            assertThat(sykedager.utledMaksdato()).isEqualTo(5 januar 2026)
        }
    }

    @ParameterizedTest
    @EnumSource(
        GjenståendeSykedagerGrunnlag::class,
        names = ["VEDTAK_I_NY_LØSNING", "SISTE_UTBETALING_I_INFOTRYGD", "MAKSDATO_I_INFOTRYGD"]
    )
    fun `bruker vurdertTilOgMed som maksdato når ingen dager gjenstår`(
        grunnlag: GjenståendeSykedagerGrunnlag
    ) {
        for (vurdertTilOgMed in listOf(2 januar 2026, 3 januar 2026, 4 januar 2026)) {
            val sykedager = GjenståendeSykedager(grunnlag, 0, vurdertTilOgMed)

            assertThat(sykedager.utledMaksdato()).isEqualTo(vurdertTilOgMed)
        }
    }

    @Test
    fun `teller helligdager som faller på mandag til fredag`() {
        val sykedager = GjenståendeSykedager(GjenståendeSykedagerGrunnlag.MAKSDATO_I_INFOTRYGD, 2, 31 desember 2025)

        assertThat(sykedager.utledMaksdato()).isEqualTo(2 januar 2026)
    }

    @ParameterizedTest
    @EnumSource(
        GjenståendeSykedagerGrunnlag::class,
        names = ["MANGLER_MAKSDATO_I_INFOTRYGD", "INGEN_SYKEPENGER_SISTE_26_UKER"]
    )
    fun `utleder ikke maksdato for grunnlag uten beregnet antall`(grunnlag: GjenståendeSykedagerGrunnlag) {
        val sykedager = GjenståendeSykedager(grunnlag, 5, 1 januar 2026)

        assertThat(sykedager.utledMaksdato()).isNull()
    }

    @ParameterizedTest
    @EnumSource(GjenståendeSykedagerGrunnlag::class)
    fun `utleder ikke maksdato når dager eller dato mangler`(grunnlag: GjenståendeSykedagerGrunnlag) {
        assertThat(
            GjenståendeSykedager(grunnlag, null, 1 januar 2026).utledMaksdato()
        ).isNull()
        assertThat(
            GjenståendeSykedager(grunnlag, 5, null).utledMaksdato()
        ).isNull()
    }
}
