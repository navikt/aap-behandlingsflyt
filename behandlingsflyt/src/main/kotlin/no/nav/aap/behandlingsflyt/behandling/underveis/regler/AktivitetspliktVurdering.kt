package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall

/** Den ferdige vurderingen av § 11-8 "Medlemmets aktivitetsplikt".
 *
 * Vurderingen kan være at det blir opphør av aap.
 *
 * Datoen/perioden som vurderingen gjelder for er implisitt, da typen brukes
 * inne i konteksten av en [Tidslinje][no.nav.aap.komponenter.tidslinje.Tidslinje].
 *
 * - [Folketrygdloven § 11-7](https://lovdata.no/lov/1997-02-28-19/§11-7)
 */

class AktivitetspliktVurdering(
    val vurdering: Aktivitetsplikt11_7Vurdering,
) {
    val vilkårsvurdering: Vilkårsvurdering
        get() = when (vurdering.utfall) {
            Utfall.STANS -> Vilkårsvurdering.BRUDD_AKTIVITETSPLIKT_11_7_STANS
            Utfall.OPPHØR -> Vilkårsvurdering.BRUDD_AKTIVITETSPLIKT_11_7_OPPHØR
            else -> throw IllegalStateException("Utfall må være STANS eller OPPHØR for å kunne mappes til Vilkårsvurdering")
        }

    enum class Vilkårsvurdering {
        BRUDD_AKTIVITETSPLIKT_11_7_STANS,
        BRUDD_AKTIVITETSPLIKT_11_7_OPPHØR;
    }
}