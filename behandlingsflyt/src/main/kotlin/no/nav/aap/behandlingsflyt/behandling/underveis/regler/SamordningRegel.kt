package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje

class SamordningRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        val samordningGraderingTidslinje =
            Tidslinje(input.samordningGrunnlag.samordningPerioder.map { Segment(it.periode, it.gradering) })

//        Periode(LocalDate., LocalDate.MAX) // gradering 0 her


//        if (input.samordningGrunnlag.foreldrePengeSluttDato) {
//            // ikke kutt til uendelig
//        }
        return resultat.leggTilVurderinger(samordningGraderingTidslinje, Vurdering::leggTilSamordningsprosent)
    }
}
