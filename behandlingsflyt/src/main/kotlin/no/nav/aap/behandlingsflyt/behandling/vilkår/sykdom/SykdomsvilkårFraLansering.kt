package no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom

import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import org.slf4j.LoggerFactory

class SykdomsvilkårFraLansering(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<SykdomsFaktagrunnlag> {
    private val vilkår: Vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
    private val log = LoggerFactory.getLogger(javaClass)

    override fun vurder(grunnlag: SykdomsFaktagrunnlag) {
        val yrkesskadeVurderingTidslinje = Tidslinje(
            Periode(grunnlag.kravDato, grunnlag.sisteDagMedMuligYtelse),
            grunnlag.yrkesskadevurdering
        )

        val sykdomsvurderingTidslinje = grunnlag.sykdomsvurderinger
            .sortedBy { it.opprettet }
            .map { vurdering ->
                Tidslinje(
                    Periode(
                        fom = vurdering.vurderingenGjelderFra,
                        tom = grunnlag.sisteDagMedMuligYtelse
                    ),
                    vurdering
                )
            }
            .fold(Tidslinje<Sykdomsvurdering>()) { t1, t2 ->
                t1.kombiner(t2, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            }

        val bistandvurderingtidslinje =
            grunnlag.bistandvurderingFaktagrunnlag
                ?.somBistandsvurderingstidslinje(grunnlag.sisteDagMedMuligYtelse)
                .orEmpty()

        val sykepengeerstatningTidslinje = grunnlag.sykepengerErstatningFaktagrunnlag?.somTidslinje(
            kravDato = grunnlag.kravDato,
            sisteMuligDagMedYtelse = grunnlag.sisteDagMedMuligYtelse
        ).orEmpty()

        val tidslinje =
            kombinerAlleTidslinjer(
                yrkesskadeVurderingTidslinje,
                sykdomsvurderingTidslinje,
                sykepengeerstatningTidslinje,
                bistandvurderingtidslinje
            )
                .mapValue { (segmentPeriode, yrkesskadeVurdering, sykdomVurdering, sykepengerVurdering, bistandVurdering) ->
                    opprettVilkårsvurdering(
                        segmentPeriode,
                        grunnlag.sykepengeerstatningVilkår,
                        sykdomVurdering,
                        yrkesskadeVurdering,
                        sykepengerVurdering,
                        bistandVurdering,
                        grunnlag
                    )
                }

        vilkår.leggTilVurderinger(tidslinje)
    }

    private fun kombinerAlleTidslinjer(
        yrkesskadeVurderingTidslinje: Tidslinje<Yrkesskadevurdering?>,
        sykdomsvurderingTidslinje: Tidslinje<Sykdomsvurdering>,
        sykepengerTidslinje: Tidslinje<SykepengerVurdering>,
        bistandvurderingTidslinje: Tidslinje<Bistandsvurdering>,
    ): Tidslinje<LokaltSegment> {
        val zip2 = Tidslinje.zip2(
            yrkesskadeVurderingTidslinje,
            sykdomsvurderingTidslinje,
        )

        return Tidslinje.map3(
            zip2,
            sykepengerTidslinje,
            bistandvurderingTidslinje,
        ) { segmentPeriode, a, b, c ->
            LokaltSegment(
                segmentPeriode,
                yrkesskadeVurdering = a?.first,
                sykdomVurdering = a?.second,
                sykepengerVurdering = b,
                bistandsvurdering = c
            )
        }
    }

    internal data class LokaltSegment(
        val segmentPeriode: Periode,
        val yrkesskadeVurdering: Yrkesskadevurdering?,
        val sykdomVurdering: Sykdomsvurdering?,
        val sykepengerVurdering: SykepengerVurdering?,
        val bistandsvurdering: Bistandsvurdering?
    )

    private fun opprettVilkårsvurdering(
        segmentPeriode: Periode,
        sykepengeerstatningVilkår: Tidslinje<Vilkårsvurdering>,
        sykdomVurdering: Sykdomsvurdering?,
        yrkesskadeVurdering: Yrkesskadevurdering?,
        sykepengerVurdering: SykepengerVurdering?,
        bistandsvurdering: Bistandsvurdering?,
        grunnlag: SykdomsFaktagrunnlag
    ): Vilkårsvurdering {
        var utfall: Utfall
        var avslagsårsak: Avslagsårsak? = null
        var innvilgelsesårsak: Innvilgelsesårsak?

        if (sykdomVurdering?.erOppfyltForYrkesskadeSettBortIfraÅrsakssammenheng(
                grunnlag.kravDato,
                segmentPeriode
            ) == true && yrkesskadeVurdering?.erÅrsakssammenheng == true
        ) {
            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG
        } else if (sykdomVurdering?.erOppfyltOrdinær(
                grunnlag.kravDato,
                segmentPeriode
            ) == true && bistandsvurdering?.erBehovForBistand() == true
        ) {
            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = null
        } else if (sykepengeerstatningVilkår.isEmpty() && sykepengerVurdering?.harRettPå == true && sykdomVurdering?.erOppfyltOrdinærSettBortIfraVissVarighet() == true) {
            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = Innvilgelsesårsak.SYKEPENGEERSTATNING
        } else {
            innvilgelsesårsak = null
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = if (sykdomVurdering?.erSkadeSykdomEllerLyteVesentligdel == false) {
                Avslagsårsak.IKKE_SYKDOM_SKADE_LYTE_VESENTLIGDEL
            } else if (sykdomVurdering?.erNedsettelseIArbeidsevneMerEnnHalvparten == false && sykdomVurdering.erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense != true) {
                Avslagsårsak.IKKE_NOK_REDUSERT_ARBEIDSEVNE
            } else if (sykdomVurdering?.erNedsettelseIArbeidsevneAvEnVissVarighet == false) {
                Avslagsårsak.IKKE_SYKDOM_AV_VISS_VARIGHET
            } else {
                // TODO: dekk alle muligheter for nei her
                Avslagsårsak.MANGLENDE_DOKUMENTASJON // TODO noe mer rett
            }

            log.info("Avslagsårsak $avslagsårsak. Er nedsettelse i arbeidsevne: ${sykdomVurdering?.erNedsettelseIArbeidsevneAvEnVissVarighet}")
        }

        return Vilkårsvurdering(
            Vilkårsperiode(
                periode = Periode(grunnlag.kravDato, grunnlag.sisteDagMedMuligYtelse),
                utfall = utfall,
                begrunnelse = null,
                innvilgelsesårsak = innvilgelsesårsak,
                avslagsårsak = avslagsårsak,
                faktagrunnlag = grunnlag,
            )
        )
    }

}