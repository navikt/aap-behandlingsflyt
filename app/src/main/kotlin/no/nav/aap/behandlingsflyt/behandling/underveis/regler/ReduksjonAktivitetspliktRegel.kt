package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ReduksjonAktivitetspliktVurdering.Vilkårsvurdering.IKKE_RELEVANT_BRUDD
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ReduksjonAktivitetspliktVurdering.Vilkårsvurdering.UNNTAK_RIMELIG_GRUNN
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ReduksjonAktivitetspliktVurdering.Vilkårsvurdering.VILKÅR_OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Paragraf.PARAGRAF_11_9
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje

/** Vurder om medlemmet kan sanksjoneres etter ftrl § 11-9 "Reduksjon av arbeidsavklaringspenger ved
 * brudd på nærmere bestemte aktivitetsplikter". Altså en implementasjon av:
 * - [Folketrygdloven § 11-9](]https://lovdata.no/lov/1997-02-28-19/§11-9)
 * - [Forkskriftens § 4](https://lovdata.no/forskrift/2017-12-13-2100/§4)
 */
class ReduksjonAktivitetspliktRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        val vurderinger = input.aktivitetspliktGrunnlag
            .tidslinje
            .mapValue { brudd ->
                when {
                    !brudd.erBruddPåNærmereBestemteAktivitetsplikter -> {
                        ReduksjonAktivitetspliktVurdering(
                            brudd = brudd,
                            vilkårsvurdering = IKKE_RELEVANT_BRUDD,
                            skalReduseres = false,
                        )
                    }
                    brudd.harRimeligGrunn -> {
                        ReduksjonAktivitetspliktVurdering(
                            brudd = brudd,
                            vilkårsvurdering = UNNTAK_RIMELIG_GRUNN,
                            skalReduseres = false,
                        )
                    }
                    else -> {
                        ReduksjonAktivitetspliktVurdering(
                            brudd = brudd,
                            vilkårsvurdering = VILKÅR_OPPFYLT,
                            skalReduseres = brudd.paragraf == PARAGRAF_11_9,
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
                    .leggTilBruddPåNærmereBestemteAktivitetsplikter(bruddSegment.verdi)
                Segment(periode, vurdering)
            },
        )
    }
}