package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje

// §11-26
class SoningRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        val soningTidslinje = konstruerTidslinje(input)
        if (soningTidslinje.isEmpty()) {
            return resultat
        }

        return resultat.kombiner(soningTidslinje,
            JoinStyle.LEFT_JOIN { periode, venstreSegment, høyreSegment ->
                var venstreVerdi = venstreSegment.verdi
                if (høyreSegment?.verdi != null) {
                    venstreVerdi = venstreVerdi.leggTilSoningsVurdering(høyreSegment.verdi)

                }
                Segment(periode, venstreVerdi)
            }
        )
    }

    private fun konstruerTidslinje(input: UnderveisInput): Tidslinje<SoningVurdering> {
        return Tidslinje(
            input.institusjonsopphold.filter {
                it.soning?.soner == true
            }.map {
                if (it.soning?.girOpphør == true) {
                    Segment(it.periode, SoningVurdering(girOpphør = true))
                } else {
                    Segment(it.periode, SoningVurdering(girOpphør = false))
                }
            }
        ).komprimer()
    }
}