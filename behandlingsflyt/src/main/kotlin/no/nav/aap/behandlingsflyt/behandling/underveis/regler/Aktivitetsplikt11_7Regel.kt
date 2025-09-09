package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Grunnlag
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode

/**
 * Vurder om medlemmet oppfyller den generelle aktivitetsplikten. Implementasjon av:
 * - [Folketrygdloven § 11-7](https://lovdata.no/lov/1997-02-28-19/§11-7)
 */
class Aktivitetsplikt11_7Regel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        return resultat.leggTilVurderinger(
            input.aktivitetsplikt11_7Grunnlag.tilAktivitetspliktVurderingTidslinje(input.rettighetsperiode),
            Vurdering::leggTilAktivitetspliktVurdering
        )
    }

    companion object {
        fun Aktivitetsplikt11_7Grunnlag.tilAktivitetspliktVurderingTidslinje(rettighetsperiode: Periode) =
            Tidslinje(
                this
                    .tidslinje()
                    .mapNotNull { segment ->
                        when (segment.verdi.erOppfylt) {
                            false -> Segment(segment.periode, AktivitetspliktVurdering(vurdering = segment.verdi))
                            true -> null
                        }
                    }).begrensetTil(rettighetsperiode)
    }
}
