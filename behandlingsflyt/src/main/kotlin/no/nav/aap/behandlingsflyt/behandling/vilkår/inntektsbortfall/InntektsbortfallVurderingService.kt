package no.nav.aap.behandlingsflyt.behandling.vilkår.inntektsbortfall

import no.nav.aap.behandlingsflyt.behandling.beregning.BeregningService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.verdityper.GUnit
import java.math.BigDecimal
import java.time.MonthDay
import java.time.Year

class InntektsbortfallVurderingService {
    fun vurderInntektsbortfall(
        kontekst: FlytKontekstMedPerioder,
        brukerPersonopplysning: Personopplysning,
        manuellInntektGrunnlag: ManuellInntektGrunnlag?,
        inntektGrunnlag: InntektGrunnlag?,
        beregningService: BeregningService
    ): InntektsbortfallKanBehandlesAutomatisk {
        val sisteRelevanteÅr = hentSisteRelevanteÅr(kontekst, beregningService)

        val under62ÅrVedSøknadstidspunktGrunnlag = erUnder62PåRettighetsperioden(kontekst, brukerPersonopplysning)
        val inntektSisteÅrOver1GGrunnlag =
            inntektSisteÅrOver1G(inntektGrunnlag, manuellInntektGrunnlag, sisteRelevanteÅr)
        val gjennomsnittInntektSiste3ÅrOver3GGrunnlag =
            gjennomsnittInntektSiste3ÅrOver3G(inntektGrunnlag, manuellInntektGrunnlag, sisteRelevanteÅr)

        return InntektsbortfallKanBehandlesAutomatisk(
            kanBehandlesAutomatisk = under62ÅrVedSøknadstidspunktGrunnlag.resultat || inntektSisteÅrOver1GGrunnlag.resultat || gjennomsnittInntektSiste3ÅrOver3GGrunnlag.resultat,
            inntektSisteÅrOver1G = inntektSisteÅrOver1GGrunnlag,
            gjennomsnittInntektSiste3ÅrOver3G = gjennomsnittInntektSiste3ÅrOver3GGrunnlag,
            under62ÅrVedSøknadstidspunkt = under62ÅrVedSøknadstidspunktGrunnlag
        )
    }

    private fun erUnder62PåRettighetsperioden(
        kontekst: FlytKontekstMedPerioder,
        brukerPersonopplysning: Personopplysning
    ): Under62ÅrVedSøknadstidspunkt {
        val alderPåStartsdato = brukerPersonopplysning.fødselsdato.alderPåDato(kontekst.rettighetsperiode.fom)
        return Under62ÅrVedSøknadstidspunkt(alderPåStartsdato, alderPåStartsdato < 62)
    }

    private fun gjennomsnittInntektSiste3ÅrOver3G(
        inntektGrunnlag: InntektGrunnlag?,
        manuellInntektGrunnlag: ManuellInntektGrunnlag?,
        sisteRelevanteÅr: Set<Year>
    ): GjennomsnittInntektSiste3ÅrOver3G {
        val inntektGrunnlagSisteRelevanteÅr = hentInntekterGrunnlag(inntektGrunnlag, sisteRelevanteÅr)
        val manuelleInntekterRelevanteÅr =
            manuellInntektGrunnlag?.manuelleInntekter?.filter { it.år in sisteRelevanteÅr } ?: emptyList()

        val manuelleMap = manuelleInntekterRelevanteÅr
            .filter { it.belop != null }
            .associateBy { it.år }


        val kombinerte = inntektGrunnlagSisteRelevanteÅr.map { inntekt ->
            manuelleMap[inntekt.år]?.let { manuell ->
                InntektPerÅr(inntekt.år, manuell.belop!!)
            } ?: inntekt
        }

        val gjennomsnitt = kombinerte
            .map { it.gUnit().gUnit.verdi() }
            .reduce(BigDecimal::add)
            .divide(BigDecimal(3))

        return GjennomsnittInntektSiste3ÅrOver3G(
            gverdi = GUnit(gjennomsnitt),
            resultat = gjennomsnitt >= BigDecimal(3)
        )
    }

    private fun inntektSisteÅrOver1G(
        inntektGrunnlag: InntektGrunnlag?,
        manuellInntektGrunnlag: ManuellInntektGrunnlag?,
        sisteRelevanteÅr: Set<Year>
    ): InntektSisteÅrOver1G {
        val inntektGrunnlagSisteRelevanteÅr =
            hentInntekterGrunnlag(inntektGrunnlag, setOf(sisteRelevanteÅr.first())).firstOrNull()
        val manuellInntektVurderingSisteRelevanteÅr =
            manuellInntektGrunnlag?.manuelleInntekter?.firstOrNull { it.år == sisteRelevanteÅr }

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
        inntektGrunnlag: InntektGrunnlag?,
        år: Set<Year>
    ): List<InntektPerÅr> {
        checkNotNull(inntektGrunnlag) {
            "Forventet å finne inntektsgrunnlag siden dette lagres i informasjonskravet."
        }
        return inntektGrunnlag.inntekter.filter { it.år in år }
    }

    private fun hentSisteRelevanteÅr(kontekst: FlytKontekstMedPerioder, beregningService: BeregningService): Set<Year> {
        val relevantBeregningsPeriode = beregningService.utledRelevanteBeregningsÅr(kontekst.behandlingId)
        val sisteÅr = relevantBeregningsPeriode.max()
        return (0L..2L).map { sisteÅr.minusYears(it) }.toSet()
    }
}