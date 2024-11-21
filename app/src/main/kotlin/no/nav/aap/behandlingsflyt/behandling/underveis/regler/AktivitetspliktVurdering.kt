package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktDokument

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
    val dokument: AktivitetspliktDokument,
    val vilkårsvurdering: Vilkårsvurdering
) {
    enum class Vilkårsvurdering {
        AKTIVT_BIDRAG_IKKE_OPPFYLT,
    }
}