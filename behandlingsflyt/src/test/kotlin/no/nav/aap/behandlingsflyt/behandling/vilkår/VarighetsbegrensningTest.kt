package no.nav.aap.behandlingsflyt.behandling.vilkår

import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VarighetsbegrensningTest {

    @Test
    fun `begrenser vurdering til én kalendermåned`() {
        val resultat = tidslinjeOf(
            Periode(1 januar 2025, 31 januar 2025) to false,
            Periode(1 februar 2025, 31 mars 2025) to true,
            Periode(1 april 2025, 30 april 2025) to false,
        ).mapMedDatoTilDatoVarighet(
            harBegrensetVarighet = { it },
            varighet = { /* en kalendermåned */ it.plusMonths(1).minusDays(1) }
        ) { varighetsvurdering, vurdering ->
            Pair(varighetsvurdering, vurdering)
        }

        assertTidslinje(
            resultat,
            Periode(1 januar 2025, 31 januar 2025) to {
                assertThat(it).isEqualTo(Pair(Varighetsvurdering.VARIGHET_OK, false))
            },
            Periode(1 februar 2025, 28 februar 2025) to {
                assertThat(it).isEqualTo(Pair(Varighetsvurdering.VARIGHET_OK, true))
            },
            Periode(1 mars 2025, 31 mars 2025) to {
                assertThat(it).isEqualTo(Pair(Varighetsvurdering.VARIGHET_OVERSKREDET, true))
            },
            Periode(1 april 2025, 30 april 2025) to {
                assertThat(it).isEqualTo(Pair(Varighetsvurdering.VARIGHET_OK, false))
            },
        )
    }

    @Test
    fun `ny vurdering gjeninntrer i eksisterende periode`() {
        val resultat = tidslinjeOf(
            Periode(1 januar 2025, 31 januar 2025) to false,
            Periode(1 februar 2025, 15 februar 2025) to true,
            Periode(16 februar 2025, 31 mars 2025) to true,
            Periode(1 april 2025, 30 april 2025) to false,
        ).mapMedDatoTilDatoVarighet(
            harBegrensetVarighet = { it },
            varighet = { /* en kalendermåned */ it.plusMonths(1).minusDays(1) }
        ) { varighetsvurdering, vurdering ->
            Pair(varighetsvurdering, vurdering)
        }

        assertTidslinje(
            resultat,
            Periode(1 januar 2025, 31 januar 2025) to {
                assertThat(it).isEqualTo(Pair(Varighetsvurdering.VARIGHET_OK, false))
            },
            Periode(1 februar 2025, 28 februar 2025) to {
                assertThat(it).isEqualTo(Pair(Varighetsvurdering.VARIGHET_OK, true))
            },
            Periode(1 mars 2025, 31 mars 2025) to {
                assertThat(it).isEqualTo(Pair(Varighetsvurdering.VARIGHET_OVERSKREDET, true))
            },
            Periode(1 april 2025, 30 april 2025) to {
                assertThat(it).isEqualTo(Pair(Varighetsvurdering.VARIGHET_OK, false))
            },
        )
    }

    @Test
    fun `støtter kun 1 dato-til-dato-periode`() {
        val resultat = tidslinjeOf(
            Periode(1 januar 2025, 31 januar 2025) to false,
            Periode(1 februar 2025, 28 februar 2025) to true,
            Periode(1 mars 2025, 31 mars 2025) to false,
            Periode(1 april 2025, 30 april 2025) to true,
        ).mapMedDatoTilDatoVarighet(
            harBegrensetVarighet = { it },
            varighet = { /* en kalendermåned */ it.plusMonths(1).minusDays(1) }
        ) { varighetsvurdering, vurdering ->
            Pair(varighetsvurdering, vurdering)
        }

        assertTidslinje(
            resultat,
            Periode(1 januar 2025, 31 januar 2025) to {
                assertThat(it).isEqualTo(Pair(Varighetsvurdering.VARIGHET_OK, false))
            },
            Periode(1 februar 2025, 28 februar 2025) to {
                assertThat(it).isEqualTo(Pair(Varighetsvurdering.VARIGHET_OK, true))
            },
            Periode(1 mars 2025, 31 mars 2025) to {
                assertThat(it).isEqualTo(Pair(Varighetsvurdering.VARIGHET_OK, false))
            },
            Periode(1 april 2025, 30 april 2025) to {
                assertThat(it).isEqualTo(Pair(Varighetsvurdering.VARIGHET_OVERSKREDET, true))
            },
        )
    }

    @Test
    fun `det er greit at ingen ting skal begrenses`() {
        val resultat = tidslinjeOf(
            Periode(1 januar 2025, 31 januar 2025) to false,
            Periode(1 februar 2025, 28 februar 2025) to false,
            Periode(1 mars 2025, 31 mars 2025) to false,
        ).mapMedDatoTilDatoVarighet(
            harBegrensetVarighet = { it },
            varighet = { /* en kalendermåned */ it.plusMonths(1).minusDays(1) }
        ) { varighetsvurdering, vurdering ->
            Pair(varighetsvurdering, vurdering)
        }

        assertTidslinje(
            resultat,
            Periode(1 januar 2025, 31 mars 2025) to {
                assertThat(it).isEqualTo(Pair(Varighetsvurdering.VARIGHET_OK, false))
            },
        )
    }
}