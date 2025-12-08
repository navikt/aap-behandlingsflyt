package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Grunnlag
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode

/**
 * Denne regelen er ikke lenger nødvendig. Det er et eget vilkår [AKTIVITETSPLIKT][no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.AKTIVITETSPLIKT] som settes
 * i [Effektuer11_7Steg][no.nav.aap.behandlingsflyt.forretningsflyt.steg.Effektuer11_7Steg].
 *
 * Kan ikke fjernes før alle åpne behandlinger med 11-7-brudd enten har kjørt Effektuer11_7-steget eller har
 * passert fatte-vedtak-steget.
 */
class Aktivitetsplikt11_7Regel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        return resultat.leggTilVurderinger(
            input.aktivitetsplikt11_7Grunnlag.tilAktivitetspliktVurderingTidslinje(input.periodeForVurdering),
            Vurdering::leggTilAktivitetspliktVurdering
        )
    }

    companion object {
        fun Aktivitetsplikt11_7Grunnlag.tilAktivitetspliktVurderingTidslinje(rettighetsperiode: Periode) =
            Tidslinje(
                this
                    .tidslinje()
                    .segmenter()
                    .mapNotNull { segment ->
                        when (segment.verdi.erOppfylt) {
                            false -> Segment(segment.periode, AktivitetspliktVurdering(vurdering = segment.verdi))
                            true -> null
                        }
                    }).begrensetTil(rettighetsperiode)
    }
}
