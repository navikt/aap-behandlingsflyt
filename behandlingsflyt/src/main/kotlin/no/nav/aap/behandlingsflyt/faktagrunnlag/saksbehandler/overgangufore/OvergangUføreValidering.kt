package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

class OvergangUføreValidering {
    companion object {
        fun nårVurderingErKonsistentMedSykdomOgBistand(
            overgangUføreTidslinje: Tidslinje<OvergangUføreVurdering>,
            sykdomstidslinje: Tidslinje<Sykdomsvurdering>,
            bistandstidslinje: Tidslinje<Bistandsvurdering>,
            kravdato: LocalDate
        ): Tidslinje<Boolean> {
            return Tidslinje.map3(
                overgangUføreTidslinje, sykdomstidslinje, bistandstidslinje
            ) { segmentPeriode, overgangUføreVurdering, sykdomsvurdering, bistandsvurdering ->
                overgangUføreVurdering == null
                        || Periode(
                    kravdato.minusMonths(8),
                    kravdato
                ).inneholder(segmentPeriode) // Det er tillatt å vurdere 11-18 før kravdato
                        || overgangUføreVurdering.brukerRettPåAAP == false // Nei-vurdering er uavhengig av bistand og sykdom
                        || sykdomErOppfyltOgBistandErIkkeOppfylt(
                    kravdato,
                    segmentPeriode,
                    sykdomsvurdering,
                    bistandsvurdering
                )
            }.komprimer()
        }

        fun sykdomErOppfyltOgBistandErIkkeOppfylt(
            kravdato: LocalDate,
            segmentPeriode: Periode,
            sykdomsvurdering: Sykdomsvurdering?,
            bistandsvurdering: Bistandsvurdering?
        ): Boolean {
            return sykdomsvurdering?.erOppfyltOrdinær(
                kravdato, segmentPeriode
            ) == true && bistandsvurdering != null && !bistandsvurdering.erBehovForBistand()
        }
    }
}