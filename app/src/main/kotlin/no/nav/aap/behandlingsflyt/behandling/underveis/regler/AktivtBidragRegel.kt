package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Paragraf.PARAGRAF_11_7
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Type.IKKE_AKTIVT_BIDRAG
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje

/**
 * Vurder om medlemmet oppfyller den generelle aktivitetsplikten. Implementasjon av:
 * - [Folketrygdloven § 11-7](https://lovdata.no/lov/1997-02-28-19/§11-7)
 */
class AktivtBidragRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        val vurderinger = input.bruddAktivitetsplikt
            .mapValue { brudd ->
                when {
                    brudd.type == IKKE_AKTIVT_BIDRAG -> {
                        assert(brudd.paragraf == PARAGRAF_11_7)
                        return@mapValue AktivtBidragVurdering(
                            brudd = brudd,
                            vilkårsvurdering = AktivtBidragVurdering.Vilkårsvurdering.VILKÅR_OPPFYLT,
                        )
                    }
                    else -> {
                        return@mapValue AktivtBidragVurdering(
                            brudd = brudd,
                            vilkårsvurdering = AktivtBidragVurdering.Vilkårsvurdering.IKKE_RELEVANT_BRUDD
                        )
                    }
                }
            }

        return vurderinger.kombiner(
            resultat,
            JoinStyle.OUTER_JOIN
            { periode, bruddSegment, vurderingSegment ->
                if (bruddSegment == null) return@OUTER_JOIN vurderingSegment
                val vurdering = (vurderingSegment?.verdi ?: Vurdering())
                    .leggTilAktivtBidragVurdering(bruddSegment.verdi)
                Segment(periode, vurdering)
            },
        )
    }
}