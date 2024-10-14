package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ReduksjonAktivitetspliktVurdering.Vilkårsvurdering.FEILREGISTRERT
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ReduksjonAktivitetspliktVurdering.Vilkårsvurdering.IKKE_RELEVANT_BRUDD
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ReduksjonAktivitetspliktVurdering.Vilkårsvurdering.UNNTAK_RIMELIG_GRUNN
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ReduksjonAktivitetspliktVurdering.Vilkårsvurdering.VILKÅR_OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Grunn
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Paragraf.PARAGRAF_11_9
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Type.IKKE_MØTT_TIL_BEHANDLING
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Type.IKKE_MØTT_TIL_MØTE
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Type.IKKE_MØTT_TIL_TILTAK
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Type.IKKE_SENDT_INN_DOKUMENTASJON
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.FeilregistrertBrudd
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje

/** Vurder om medlemmet kan sanksjoneres etter ftrl § 11-9 "Reduksjon av arbeidsavklaringspenger ved
 * brudd på nærmere bestemte aktivitetsplikter". Altså en implementasjon av:
 * - [Folketrygdloven § 11-9](]https://lovdata.no/lov/1997-02-28-19/§11-9)
 * - [Forkskriftens § 4](https://lovdata.no/forskrift/2017-12-13-2100/§4)
 */
class ReduksjonAktivitetspliktRegel : UnderveisRegel {
    companion object {
        val relevanteBrudd = listOf(
            IKKE_MØTT_TIL_MØTE,
            IKKE_MØTT_TIL_BEHANDLING,
            IKKE_MØTT_TIL_TILTAK,
            IKKE_SENDT_INN_DOKUMENTASJON,
        )

        val gyldigeGrunner = listOf(
            Grunn.SYKDOM_ELLER_SKADE,
            Grunn.STERKE_VELFERDSGRUNNER,
            Grunn.RIMELIG_GRUNN,
        )
    }

    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        val vurderinger = input.aktivitetspliktGrunnlag
            .tidslinje
            .mapValue { dokument ->
                when (dokument) {
                    is FeilregistrertBrudd -> {
                        ReduksjonAktivitetspliktVurdering(
                            dokument = dokument,
                            vilkårsvurdering = FEILREGISTRERT,
                            skalReduseres = false,
                        )
                    }

                    is BruddAktivitetsplikt -> {
                        when {
                            dokument.type !in relevanteBrudd -> {
                                ReduksjonAktivitetspliktVurdering(
                                    dokument = dokument,
                                    vilkårsvurdering = IKKE_RELEVANT_BRUDD,
                                    skalReduseres = false,
                                )
                            }

                            dokument.grunn in gyldigeGrunner -> {
                                ReduksjonAktivitetspliktVurdering(
                                    dokument = dokument,
                                    vilkårsvurdering = UNNTAK_RIMELIG_GRUNN,
                                    skalReduseres = false,
                                )
                            }

                            else -> {
                                ReduksjonAktivitetspliktVurdering(
                                    dokument = dokument,
                                    vilkårsvurdering = VILKÅR_OPPFYLT,
                                    skalReduseres = dokument.paragraf == PARAGRAF_11_9,
                                )
                            }
                        }
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