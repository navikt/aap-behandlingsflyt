package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VurdertBarnTest {
    // Test for klasse VurdertBarn
    @Test
    fun `tilTidslinje() bruker Tid MAKS som sluttdato for BarnIdent`() {
        val ident = BarnIdentifikator.BarnIdent("12345678901")
        val vurdering = VurderingAvForeldreAnsvar(
            fraDato = LocalDate.of(2023, 1, 1),
            harForeldreAnsvar = true,
            begrunnelse = "Begrunnelse",
            erFosterForelder = false
        )
        val vurdertBarn = VurdertBarn(ident, listOf(vurdering))

        val tidslinje = vurdertBarn.tilTidslinje()

        val perioder = tidslinje.perioder()
        assertEquals(1, perioder.count())
        assertEquals(LocalDate.of(2023, 1, 1), perioder.first().fom)
        assertEquals(Tid.MAKS, perioder.first().tom)
        val verdi = tidslinje.segment(LocalDate.of(2023, 1, 1))
        assertTrue(verdi?.verdi?.harForeldreAnsvar == true)
    }

    @Test
    fun `tilTidslinje() bruker periodeMedRettTil som sluttdato for NavnOgFødselsdato`() {
        val fødselsdato = Fødselsdato(LocalDate.of(2020, 1, 1))
        val ident = BarnIdentifikator.NavnOgFødselsdato("Ole Hansen", fødselsdato)
        val vurdering = VurderingAvForeldreAnsvar(
            fraDato = LocalDate.of(2023, 1, 1),
            harForeldreAnsvar = true,
            begrunnelse = "Begrunnelse",
            erFosterForelder = false
        )
        val vurdertBarn = VurdertBarn(ident, listOf(vurdering))

        val tidslinje = vurdertBarn.tilTidslinje()

        val perioder = tidslinje.perioder()
        val forventetTom = Barn.periodeMedRettTil(fødselsdato, null).tom
        assertEquals(1, perioder.count())
        assertEquals(LocalDate.of(2023, 1, 1), perioder.first().fom)
        assertEquals(forventetTom, perioder.first().tom)
    }

    @Test
    fun `tilTidslinje() sorterer vurderinger etter fraDato`() {
        val ident = BarnIdentifikator.BarnIdent("12345678901")
        val vurdering1 = VurderingAvForeldreAnsvar(
            fraDato = LocalDate.of(2023, 6, 1),
            harForeldreAnsvar = true,
            begrunnelse = "Senere",
            erFosterForelder = false
        )
        val vurdering2 = VurderingAvForeldreAnsvar(
            fraDato = LocalDate.of(2023, 1, 1),
            harForeldreAnsvar = false,
            begrunnelse = "Tidligere",
            erFosterForelder = null
        )
        val vurdertBarn = VurdertBarn(ident, listOf(vurdering1, vurdering2))

        val tidslinje = vurdertBarn.tilTidslinje()

        assertFalse(tidslinje.segment(LocalDate.of(2023, 5, 1))!!.verdi.harForeldreAnsvar)
        assertTrue(tidslinje.segment(LocalDate.of(2023, 7, 1))!!.verdi.harForeldreAnsvar)
    }

    @Test
    fun `tilTidslinje() komprimerer perioder med samme vurdering`() {
        val ident = BarnIdentifikator.BarnIdent("12345678901")
        val vurdering1 = VurderingAvForeldreAnsvar(
            fraDato = LocalDate.of(2023, 1, 1),
            harForeldreAnsvar = true,
            begrunnelse = "Begrunnelse",
            erFosterForelder = false
        )
        val vurdering2 = VurderingAvForeldreAnsvar(
            fraDato = LocalDate.of(2023, 3, 1),
            harForeldreAnsvar = true,
            begrunnelse = "Begrunnelse",
            erFosterForelder = false
        )
        val vurdertBarn = VurdertBarn(ident, listOf(vurdering1, vurdering2))

        val tidslinje = vurdertBarn.tilTidslinje()

        assertEquals(1, tidslinje.perioder().count())
    }

    @Test
    fun `tilTidslinje() returnerer tom tidslinje for tom liste av vurderinger`() {
        val ident = BarnIdentifikator.BarnIdent("12345678901")
        val vurdertBarn = VurdertBarn(ident, emptyList())

        val tidslinje = vurdertBarn.tilTidslinje()

        assertThat(tidslinje.perioder().toList()).isEmpty()
    }

    // Test for klasse BarnIdentifikator
    @Test
    fun `NavnOgFødselsdato sammenligner navn case-insensitive`() {
        val barn1 = BarnIdentifikator.NavnOgFødselsdato("Ole Hansen", Fødselsdato(LocalDate.of(2020, 1, 1)))
        val barn2 = BarnIdentifikator.NavnOgFødselsdato("ole hansen", Fødselsdato(LocalDate.of(2020, 1, 1)))

        assertThat(barn1.compareTo(barn2)).isEqualTo(0)
    }

    @Test
    fun `BarnIdent og NavnOgFødselsdato sammenligner identifikator mot navn`() {
        val barnIdent = BarnIdentifikator.BarnIdent(Ident("12345678901"))
        val navnOgFødselsdato = BarnIdentifikator.NavnOgFødselsdato("Ole Hansen", Fødselsdato(LocalDate.of(2020, 1, 1)))

        assertNotEquals(0, barnIdent.compareTo(navnOgFødselsdato))
        assertNotEquals(0, navnOgFødselsdato.compareTo(barnIdent))
    }

    @Test
    fun `er() returnerer true når objekter er like`() {
        val ident1 = BarnIdentifikator.BarnIdent(Ident("12345678901"))
        val ident2 = BarnIdentifikator.BarnIdent(Ident("12345678901"))

        assertTrue(ident1.er(ident2))
    }

    @Test
    fun `er() returnerer true når compareTo returnerer 0`() {
        val barn1 = BarnIdentifikator.NavnOgFødselsdato("Ole Hansen", Fødselsdato(LocalDate.of(2020, 1, 1)))
        val barn2 = BarnIdentifikator.NavnOgFødselsdato("ole hansen", Fødselsdato(LocalDate.of(2020, 1, 1)))

        assertTrue(barn1.er(barn2))
    }

    @Test
    fun `er() returnerer false når objekter er forskjellige`() {
        val ident = BarnIdentifikator.BarnIdent(Ident("12345678901"))
        val navnOgFødselsdato = BarnIdentifikator.NavnOgFødselsdato("Ole Hansen", Fødselsdato(LocalDate.of(2020, 1, 1)))

        assertFalse(ident.er(navnOgFødselsdato))
    }

    @Test
    fun `BarnIdent konstruktør med String lager riktig objekt`() {
        val ident = BarnIdentifikator.BarnIdent("12345678901")

        assertEquals("12345678901", ident.ident.identifikator)
    }

}