package no.nav.aap.behandlingsflyt.behandling.vilkår.inntektsbortfall

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.GUnit
import java.math.BigDecimal
import java.time.MonthDay
import java.time.Year

class InntektsbortfallVurderingService(
    private val relevanteBeregningsår: Set<Year>,
    private val rettighetsperiode: Periode
) {
    fun vurderInntektsbortfall(
        fødselsdato: Fødselsdato,
        inntektPerÅr: Set<InntektPerÅr>
    ): InntektsbortfallKanBehandlesAutomatisk {
        val sisteRelevanteÅr = hentSisteRelevanteÅr()
        val under62ÅrVedSøknadstidspunktGrunnlag = erUnder62PåRettighetsperioden(fødselsdato)
        val inntektSisteÅrOver1GGrunnlag =
            inntektSisteÅrOver1G(inntektPerÅr, sisteRelevanteÅr)
        val inntektSiste3ÅrOver3GGrunnlag =
            inntektSiste3ÅrOver3G(inntektPerÅr, sisteRelevanteÅr)

        return InntektsbortfallKanBehandlesAutomatisk(
            kanBehandlesAutomatisk = under62ÅrVedSøknadstidspunktGrunnlag.resultat || inntektSisteÅrOver1GGrunnlag.resultat || inntektSiste3ÅrOver3GGrunnlag.resultat,
            inntektSisteÅrOver1G = inntektSisteÅrOver1GGrunnlag,
            inntektSiste3ÅrOver3G = inntektSiste3ÅrOver3GGrunnlag,
            under62ÅrVedSøknadstidspunkt = under62ÅrVedSøknadstidspunktGrunnlag
        )
    }

    private fun erUnder62PåRettighetsperioden(
        fødselsdato: Fødselsdato
    ): Under62ÅrVedSøknadstidspunkt {
        val alderPåStartsdato = fødselsdato.alderPåDato(rettighetsperiode.fom)
        return Under62ÅrVedSøknadstidspunkt(alderPåStartsdato, alderPåStartsdato < 62)
    }

    private fun inntektSiste3ÅrOver3G(
        inntektGrunnlag: Set<InntektPerÅr>,
        sisteRelevanteÅr: Set<Year>
    ): InntektSiste3ÅrOver3G {
        val inntektGrunnlagSisteRelevanteÅr = hentInntekterGrunnlag(inntektGrunnlag, sisteRelevanteÅr)

        return if (inntektGrunnlagSisteRelevanteÅr.isEmpty()) {
            InntektSiste3ÅrOver3G(gverdi = GUnit(BigDecimal.ZERO), resultat = false)
        } else {
            val gjennomsnitt = inntektGrunnlagSisteRelevanteÅr
                .map { it.gUnit().gUnit.verdi() }
                .reduce(BigDecimal::add)

            InntektSiste3ÅrOver3G(
                gverdi = GUnit(gjennomsnitt),
                resultat = gjennomsnitt >= BigDecimal(3)
            )
        }
    }

    private fun inntektSisteÅrOver1G(
        inntektPerÅr: Set<InntektPerÅr>,
        sisteRelevanteÅr: Set<Year>
    ): InntektSisteÅrOver1G {
        // Greit å ta firstOrNull her, siden den vil returnere maks ett element. Så svaret er veldefinert.
        val inntektGrunnlagSisteRelevanteÅr =
            hentInntekterGrunnlag(inntektPerÅr, setOf(sisteRelevanteÅr.max())).firstOrNull()

        val inntektsGrunnlag = inntektGrunnlagSisteRelevanteÅr?.beløp

        if (inntektsGrunnlag != null) {
            val startPåRelevantÅr = Year.of(sisteRelevanteÅr.first().value).atMonthDay(MonthDay.of(1, 1))
            val gVerdi = Grunnbeløp.finnGUnit(startPåRelevantÅr, inntektsGrunnlag)
            val har1GSisteÅretEllerOver = gVerdi.gUnit.verdi() >= BigDecimal.ONE

            return InntektSisteÅrOver1G(gVerdi.gUnit, har1GSisteÅretEllerOver)
        }
        return InntektSisteÅrOver1G(GUnit(BigDecimal.ZERO), false)
    }

    /**
     * Filtrer [inntektPerÅr] basert på [år]. Så størrelsen er alltid mindre enn eller lik input.
     */
    private fun hentInntekterGrunnlag(
        inntektPerÅr: Set<InntektPerÅr>,
        år: Set<Year>
    ): List<InntektPerÅr> {
        return inntektPerÅr.filter { it.år in år }.sortedBy { it.år }
    }

    private fun hentSisteRelevanteÅr(): Set<Year> {
        val sisteÅr = if (relevanteBeregningsår.isNotEmpty()) {
            relevanteBeregningsår.max()
        } else {
            Year.of(rettighetsperiode.fom.year - 1)
        }

        return (0L..2L).map { sisteÅr.minusYears(it) }.toSet()
    }
}