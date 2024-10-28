package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.AktivtBidragVurdering.Vilkårsvurdering.VILKÅR_OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_7
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType
import no.nav.aap.tidslinje.Tidslinje

/**
 * Vurder om medlemmet oppfyller den generelle aktivitetsplikten. Implementasjon av:
 * - [Folketrygdloven § 11-7](https://lovdata.no/lov/1997-02-28-19/§11-7)
 */
class AktivtBidragRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        val vurderinger = input.aktivitetspliktGrunnlag
            .tidslinje(PARAGRAF_11_7)
            .mapValue { dokument ->
                require(dokument.brudd.bruddType == BruddType.IKKE_AKTIVT_BIDRAG) {
                    "Paragraf 11-7 har kun mulighet til å registrere med IKKE_AKTIVT_BIDRAG, men fikk ${dokument.brudd.bruddType}"
                }
                AktivtBidragVurdering(
                    dokument = dokument,
                    vilkårsvurdering = VILKÅR_OPPFYLT,
                )
            }

        return resultat.leggTilVurderinger(vurderinger, Vurdering::leggTilAktivtBidragVurdering)
    }
}