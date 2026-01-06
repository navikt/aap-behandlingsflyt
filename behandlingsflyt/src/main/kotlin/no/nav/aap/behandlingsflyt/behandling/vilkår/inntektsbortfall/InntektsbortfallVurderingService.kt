package no.nav.aap.behandlingsflyt.behandling.vilkår.inntektsbortfall

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.verdityper.GUnit
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.MonthDay
import java.time.Year

class InntektsbortfallVurderingService(
    private val kontekst: FlytKontekstMedPerioder,
    private val relevanteBeregningsår: Set<Year>
) {
    fun vurderInntektsbortfall(
        fødselsdato: Fødselsdato,
        manuelleInntekter: Set<ManuellInntektVurdering>,
        inntektPerÅr: Set<InntektPerÅr>?
    ): InntektsbortfallKanBehandlesAutomatisk {
        val sisteRelevanteÅr = hentSisteRelevanteÅr()
        val under62ÅrVedSøknadstidspunktGrunnlag = erUnder62PåRettighetsperioden(fødselsdato)
        val inntektSisteÅrOver1GGrunnlag =
            inntektSisteÅrOver1G(inntektPerÅr, manuelleInntekter, sisteRelevanteÅr)
        val gjennomsnittInntektSiste3ÅrOver3GGrunnlag =
            gjennomsnittInntektSiste3ÅrOver3G(inntektPerÅr, manuelleInntekter, sisteRelevanteÅr)

        return InntektsbortfallKanBehandlesAutomatisk(
            kanBehandlesAutomatisk = under62ÅrVedSøknadstidspunktGrunnlag.resultat || inntektSisteÅrOver1GGrunnlag.resultat || gjennomsnittInntektSiste3ÅrOver3GGrunnlag.resultat,
            inntektSisteÅrOver1G = inntektSisteÅrOver1GGrunnlag,
            gjennomsnittInntektSiste3ÅrOver3G = gjennomsnittInntektSiste3ÅrOver3GGrunnlag,
            under62ÅrVedSøknadstidspunkt = under62ÅrVedSøknadstidspunktGrunnlag
        )
    }

    private fun erUnder62PåRettighetsperioden(
        fødselsdato: Fødselsdato
    ): Under62ÅrVedSøknadstidspunkt {
        val alderPåStartsdato = fødselsdato.alderPåDato(kontekst.rettighetsperiode.fom)
        return Under62ÅrVedSøknadstidspunkt(alderPåStartsdato, alderPåStartsdato < 62)
    }

    private fun gjennomsnittInntektSiste3ÅrOver3G(
        inntektGrunnlag: Set<InntektPerÅr>?,
        manuelleInntekter: Set<ManuellInntektVurdering>,
        sisteRelevanteÅr: Set<Year>
    ): GjennomsnittInntektSiste3ÅrOver3G {
        val inntektGrunnlagSisteRelevanteÅr = hentInntekterGrunnlag(inntektGrunnlag, sisteRelevanteÅr)
        val manuelleInntekterRelevanteÅr =
            manuelleInntekter.filter { it.år in sisteRelevanteÅr }

        val manuelleMap = manuelleInntekterRelevanteÅr
            .filter { it.belop != null }
            .associateBy { it.år }


        val kombinerte = inntektGrunnlagSisteRelevanteÅr.map { inntekt ->
            manuelleMap[inntekt.år]?.let { manuell ->
                InntektPerÅr(inntekt.år, manuell.belop!!)
            } ?: inntekt
        }

        return if (kombinerte.isEmpty()) {
            GjennomsnittInntektSiste3ÅrOver3G(gverdi = GUnit(BigDecimal.ZERO), resultat = false)
        } else {
            val gjennomsnitt = kombinerte
                .map { it.gUnit().gUnit.verdi() }
                .reduce(BigDecimal::add)
                .divide(BigDecimal(3), RoundingMode.HALF_UP)

            GjennomsnittInntektSiste3ÅrOver3G(
                gverdi = GUnit(gjennomsnitt),
                resultat = gjennomsnitt >= BigDecimal(3)
            )
        }
    }

    private fun inntektSisteÅrOver1G(
        inntektGrunnlag: Set<InntektPerÅr>?,
        manuelleInntekter: Set<ManuellInntektVurdering>,
        sisteRelevanteÅr: Set<Year>
    ): InntektSisteÅrOver1G {
        // FIXME set har ikke rekkefølge!
        val inntektGrunnlagSisteRelevanteÅr =
            hentInntekterGrunnlag(inntektGrunnlag, setOf(sisteRelevanteÅr.max())).firstOrNull()
        val manuellInntektVurderingSisteRelevanteÅr =
            manuelleInntekter.firstOrNull { it.år == sisteRelevanteÅr }

        val inntektsGrunnlag = manuellInntektVurderingSisteRelevanteÅr?.belop ?: inntektGrunnlagSisteRelevanteÅr?.beløp

        if (inntektsGrunnlag != null) {
            val startPåRelevantÅr = Year.of(sisteRelevanteÅr.first().value).atMonthDay(MonthDay.of(1, 1))
            val gVerdi = Grunnbeløp.finnGUnit(startPåRelevantÅr, inntektsGrunnlag)
            val har1GSisteÅretEllerOver = gVerdi.gUnit.verdi() >= BigDecimal.ONE

            return InntektSisteÅrOver1G(gVerdi.gUnit, har1GSisteÅretEllerOver)
        }
        return InntektSisteÅrOver1G(GUnit(BigDecimal.ZERO), false)
    }

    private fun hentInntekterGrunnlag(
        inntektPerÅr: Set<InntektPerÅr>?,
        år: Set<Year>
    ): List<InntektPerÅr> {
        if (inntektPerÅr == null) return emptyList()
        return inntektPerÅr.filter { it.år in år }
    }

    private fun hentSisteRelevanteÅr(): Set<Year> {
        val sisteÅr = if (relevanteBeregningsår.isNotEmpty()) {
            relevanteBeregningsår.max()
        } else {
            Year.of(kontekst.rettighetsperiode.fom.year - 1)
        }

        return (0L..2L).map { sisteÅr.minusYears(it) }.toSet()
    }
}