package no.nav.aap.behandlingsflyt.steg.underveis.regler

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.underveis.Vurdering
import no.nav.aap.underveis.leggTilVurderinger

class UtledMeldeperiodeRegel : UnderveisRegel {
    companion object {
        const val MELDEPERIODE_LENGDE: Long = 14
    }

    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        val meldeperiodeTidslinje = Tidslinje(input.meldeperioder.map { Segment(it, it) })
            .begrensetTil(input.periodeForVurdering)

        return resultat.leggTilVurderinger(meldeperiodeTidslinje, Vurdering::leggTilMeldeperiode)
    }
}