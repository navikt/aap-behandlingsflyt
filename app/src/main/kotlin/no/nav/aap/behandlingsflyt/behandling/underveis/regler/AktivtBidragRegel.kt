package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.AktivtBidragVurdering.Vilkårsvurdering.IKKE_RELEVANT_BRUDD
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.AktivtBidragVurdering.Vilkårsvurdering.VILKÅR_OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Brudd.IKKE_AKTIVT_BIDRAG
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Paragraf.PARAGRAF_11_7
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje

/**
 * Vurder om medlemmet oppfyller den generelle aktivitetsplikten. Implementasjon av:
 * - [Folketrygdloven § 11-7](https://lovdata.no/lov/1997-02-28-19/§11-7)
 */
class AktivtBidragRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        val vurderinger = input.aktivitetspliktGrunnlag
            .tidslinje
            .mapValue { dokument ->
                when {
                    dokument.brudd == IKKE_AKTIVT_BIDRAG -> {
                        assert(dokument.paragraf == PARAGRAF_11_7)
                        AktivtBidragVurdering(
                            dokument = dokument,
                            vilkårsvurdering = VILKÅR_OPPFYLT,
                        )
                    }

                    else -> AktivtBidragVurdering(
                        dokument = dokument,
                        vilkårsvurdering = IKKE_RELEVANT_BRUDD
                    )
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