package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.AktivitetspliktVurdering.Vilkårsvurdering.AKTIVT_BIDRAG_IKKE_OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_7
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode

/**
 * Vurder om medlemmet oppfyller den generelle aktivitetsplikten. Implementasjon av:
 * - [Folketrygdloven § 11-7](https://lovdata.no/lov/1997-02-28-19/§11-7)
 */
class AktivitetspliktRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        val vurderinger = input.aktivitetspliktGrunnlag
            .tidslinje(PARAGRAF_11_7)
            .map { dokumentSegment ->
                val dokument = dokumentSegment.verdi
                require(dokument.brudd.bruddType == BruddType.IKKE_AKTIVT_BIDRAG) {
                    "Paragraf 11-7 har kun mulighet til å registrere med IKKE_AKTIVT_BIDRAG, men fikk ${dokument.brudd.bruddType}"
                }
                Tidslinje(
                    Periode(dokumentSegment.periode.fom, input.rettighetsperiode.tom),
                    AktivitetspliktVurdering(
                        dokument = dokument,
                        vilkårsvurdering = AKTIVT_BIDRAG_IKKE_OPPFYLT
                    )
                )
            }.fold(Tidslinje<AktivitetspliktVurdering>()) { tidligereVurderinger, nyereVurdering ->
                tidligereVurderinger.kombiner(nyereVurdering, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            }

        return resultat.leggTilVurderinger(vurderinger, Vurdering::leggTilAktivtBidragVurdering)
    }
}