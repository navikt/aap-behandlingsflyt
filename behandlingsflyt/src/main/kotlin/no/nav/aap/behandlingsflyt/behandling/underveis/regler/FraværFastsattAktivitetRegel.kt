package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.REDUKSJON_ANDRE_DAG
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.REDUKSJON_TI_DAGER_BRUKT_OPP
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_INNTIL_EN_DAG
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_STERKE_VELFERDSGRUNNER
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_SYKDOM_ELLER_SKADE
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.FraværForDag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.FraværÅrsak
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode

private const val KVOTE_KALENDERÅR = 10
private const val KVOTE_BARN = 3
private const val KVOTE_DØDSFALL = 3


/** Vurder om medlemmet kan sanksjoneres etter § 11-8 "Fravær fra fastsatt aktivitet".
 *
 * Implementasjon av:
 * - [Folketrygdloven § 11-8](https://lovdata.no/lov/1997-02-28-19/§11-8)
 * - [Forskriftens § 3](https://lovdata.no/forskrift/2017-12-13-2100/§3)
 */
class FraværFastsattAktivitetRegel : UnderveisRegel {
    companion object {
        private val gyldigeÅrsaker = listOf(
            FraværÅrsak.OMSORG_FØRSTE_SKOLEDAG_TILVENNING_ELLER_ANNEN_OPPFØLGING_BARN,
            FraværÅrsak.OMSORG_PLEIE_I_HJEMMET_AV_NÆR_PÅRØRENDE,
            FraværÅrsak.OMSORG_DØDSFALL_I_FAMILIE_ELLER_VENNEKRETS,
            FraværÅrsak.OMSORG_ANNEN_STERK_GRUNN,

            FraværÅrsak.SYKDOM_ELLER_SKADE,
        )
    }

    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        require(input.periodeForVurdering.inneholder(resultat.helePerioden())) {
            "kan ikke vurdere utenfor periode for vurdering fordi meldeperioden ikke er definert"
        }

        val fraværTidslinje: Tidslinje<FraværForDag> =
            input.meldekort
                .sortedBy { it.mottattTidspunkt } // somTidslinje vil overskrive tidligere verdier
                .flatMap { it.fravær }.somTidslinje { Periode(it.dato, it.dato) }

        // Første brudd i meldeperioden teller ikke i årskvote
        // Deler opp på meldeperiode først for å finne første i meldeperioden
        // Dette brukes for å regne ut antall brudd per kalenderår
        val fraværTidslinjeMedFørsteFraværIdentifisert: Tidslinje<FraværForDagVurdertForPeriode> =
            tidslinjeMedFørsteFraværIdentifisert(input.meldeperioder, fraværTidslinje)

        val ferdigVurdert = fraværTidslinjeMedFørsteFraværIdentifisert.splittOppKalenderår()
            .flatMap { kalenderårSegment ->
                vurderKalenderår(kalenderårSegment.verdi)
            }.begrensetTil(input.periodeForVurdering) // TODO skal dette gjøres?

        return resultat.leggTilVurderinger(ferdigVurdert, Vurdering::leggTilAktivitetspliktVurdering)
    }

    private fun tidslinjeMedFørsteFraværIdentifisert(
        meldeperioder: List<Periode>,
        fravær: Tidslinje<FraværForDag>,
    ): Tidslinje<FraværForDagVurdertForPeriode> {
        return fravær.splittOppIPerioder(meldeperioder)
            .flatMap { meldeperiodenSegment ->
                vurderMeldeperiode(meldeperiodenSegment.verdi)
            }
    }

    /**
     * [meldeperioden] er en tidslinje som bare skal inneholde segment med periode innenfor en gitt meldeperiode.
     */
    private fun vurderMeldeperiode(meldeperioden: Tidslinje<FraværForDag>): Tidslinje<FraværForDagVurdertForPeriode> {
        return meldeperioden.flatMap { fraværSegment ->
            vurderMeldeperiode(meldeperioden, fraværSegment.periode, fraværSegment.verdi)
        }
    }

    private fun vurderMeldeperiode(
        meldeperioden: Tidslinje<FraværForDag>,
        periode: Periode,
        fravær: FraværForDag,
    ): Tidslinje<FraværForDagVurdertForPeriode> {
        val inntilEnDagUnntakUtenGyldigGrunn = meldeperioden.segmenter().firstOrNull {
            it.verdi.fraværÅrsak !in gyldigeÅrsaker
        }?.verdi

        val harInntilEnDagUnntak = inntilEnDagUnntakUtenGyldigGrunn == fravær
        return if (harInntilEnDagUnntak) {
            val førsteFravær = Periode(periode.fom, periode.fom)
            val periodene = listOf(førsteFravær) + periode.minus(førsteFravær)
            Tidslinje(
                periodene.map {
                    Segment(
                        it,
                        FraværForDagVurdertForPeriode(
                            fravær = fravær,
                            erUnntakForDag = it == førsteFravær,
                        )
                    )
                }
            )
        } else {
            Tidslinje(
                periode,
                FraværForDagVurdertForPeriode(
                    fravær = fravær,
                    erUnntakForDag = false,
                )
            )
        }
    }

    /**
     * [kalenderår] er en tidslinje som bare skal inneholde segment med periode innenfor et gitt år.
     */
    private fun vurderKalenderår(kalenderår: Tidslinje<FraværForDagVurdertForPeriode>): Tidslinje<FraværFastsattAktivitetVurdering> {
        var kalenderårskvote = 0

        return kalenderår.flatMap { vurderingSegment ->
            val vurdering = vurderingSegment.verdi
            val fravær = vurdering.fravær

            when (fravær.fraværÅrsak) {
                FraværÅrsak.SYKDOM_ELLER_SKADE ->
                    Tidslinje(
                        vurderingSegment.periode,
                        FraværFastsattAktivitetVurdering(
                            fravær = fravær,
                            vilkårsvurdering = UNNTAK_SYKDOM_ELLER_SKADE,
                        )
                    )

                FraværÅrsak.OMSORG_FØRSTE_SKOLEDAG_TILVENNING_ELLER_ANNEN_OPPFØLGING_BARN, // TODO kvote
                FraværÅrsak.OMSORG_PLEIE_I_HJEMMET_AV_NÆR_PÅRØRENDE,
                FraværÅrsak.OMSORG_DØDSFALL_I_FAMILIE_ELLER_VENNEKRETS, // TODO kvote
                FraværÅrsak.OMSORG_ANNEN_STERK_GRUNN -> {
                    (0..<vurderingSegment.periode.antallDager()).map { periodeOffset ->
                        kalenderårskvote += 1
                        val dag = vurderingSegment.periode.fom.plusDays(periodeOffset.toLong())
                        val periode = Periode(dag, dag)
                        Segment(
                            periode,
                            FraværFastsattAktivitetVurdering(
                                fravær = fravær,
                                vilkårsvurdering =
                                    if (kalenderårskvote > KVOTE_KALENDERÅR) REDUKSJON_TI_DAGER_BRUKT_OPP
                                    else UNNTAK_STERKE_VELFERDSGRUNNER,
                            )
                        )
                    }
                        .let { Tidslinje(it) }
                        .komprimer()
                }

                FraværÅrsak.ANNET ->
                    Tidslinje(
                        vurderingSegment.periode,
                        FraværFastsattAktivitetVurdering(
                            fravær = fravær,
                            vilkårsvurdering =
                                if (vurdering.erUnntakForDag) UNNTAK_INNTIL_EN_DAG
                                else REDUKSJON_ANDRE_DAG,
                        )
                    )
            }
        }
    }


    class FraværForDagVurdertForPeriode(
        val fravær: FraværForDag,
        val erUnntakForDag: Boolean,
    )
}
