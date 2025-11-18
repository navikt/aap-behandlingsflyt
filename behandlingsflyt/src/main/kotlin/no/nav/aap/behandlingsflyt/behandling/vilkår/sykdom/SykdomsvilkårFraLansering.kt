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
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
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
        val studentVurderingTidslinje = Tidslinje(
            Periode(grunnlag.kravDato, grunnlag.sisteDagMedMuligYtelse),
            grunnlag.studentvurdering
        )

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

        val sykepengeerstatningTidslinje = grunnlag.sykepengerErstatningFaktagrunnlag?.somTidslinje(
            kravDato = grunnlag.kravDato,
            sisteMuligDagMedYtelse = grunnlag.sisteDagMedMuligYtelse
        ).orEmpty()

        val bistandvurderingtidslinje =
            grunnlag.bistandvurderingFaktagrunnlag
                ?.somBistandsvurderingstidslinje(grunnlag.sisteDagMedMuligYtelse)
                .orEmpty()

        val tidslinje =
            kombinerAlleTidslinjer(
                studentVurderingTidslinje,
                yrkesskadeVurderingTidslinje,
                sykdomsvurderingTidslinje,
                sykepengeerstatningTidslinje,
                bistandvurderingtidslinje
            )
                .mapValue { (studentVurdering, yrkesskadeVurdering, sykdomVurdering, sykepengerVurdering, bistandVurdering) ->
                    opprettVilkårsvurdering(
                        studentVurdering,
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
        studentVurderingTidslinje: Tidslinje<StudentVurdering?>,
        yrkesskadeVurderingTidslinje: Tidslinje<Yrkesskadevurdering?>,
        sykdomsvurderingTidslinje: Tidslinje<Sykdomsvurdering>,
        sykepengerVurderingTidslinje: Tidslinje<SykepengerVurdering>,
        bistandvurderingTidslinje: Tidslinje<Bistandsvurdering>,
    ): Tidslinje<LokaltSegment> {
        val zip3 = Tidslinje.zip3(
            studentVurderingTidslinje,
            yrkesskadeVurderingTidslinje,
            sykdomsvurderingTidslinje,
        )

        return Tidslinje.zip3(
            zip3,
            sykepengerVurderingTidslinje,
            bistandvurderingTidslinje,
        ).mapValue { (a, b, c) ->
            LokaltSegment(
                studentVurdering = a?.first,
                yrkesskadeVurdering = a?.second,
                sykdomVurdering = a?.third,
                sykepengerVurdering = b,
                bistandsvurdering = c
            )
        }
    }

    internal data class LokaltSegment(
        val studentVurdering: StudentVurdering?,
        val yrkesskadeVurdering: Yrkesskadevurdering?,
        val sykdomVurdering: Sykdomsvurdering?,
        val sykepengerVurdering: SykepengerVurdering?,
        val bistandsvurdering: Bistandsvurdering?
    )

    private fun opprettVilkårsvurdering(
        studentVurdering: StudentVurdering?,
        sykdomVurdering: Sykdomsvurdering?,
        yrkesskadeVurdering: Yrkesskadevurdering?,
        sykepengerVurdering: SykepengerVurdering?,
        bistandsvurdering: Bistandsvurdering?,
        grunnlag: SykdomsFaktagrunnlag
    ): Vilkårsvurdering {
        var utfall: Utfall
        var avslagsårsak: Avslagsårsak? = null
        var innvilgelsesårsak: Innvilgelsesårsak?

        if (studentVurdering?.erOppfylt() == true) {
            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = Innvilgelsesårsak.STUDENT
        } else if (sykdomVurdering?.erOppfyltForYrkesskade() == true && yrkesskadeVurdering?.erÅrsakssammenheng == true) {
            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG
        } else if (sykdomVurdering?.erOppfylt(grunnlag.kravDato) == true && bistandsvurdering?.erBehovForBistand() == true) {
            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = null
        } else if (sykepengerVurdering?.harRettPå == true && sykdomVurdering?.erOppfyltSettBortIfraVissVarighet() == true) {
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

            log.info("Avslagsårsak $avslagsårsak. Er nedsettelse i arbeidsevne: ${sykdomVurdering?.erNedsettelseIArbeidsevneAvEnVissVarighet}. Har rett på sykepengeerstatning: ${sykepengerVurdering?.harRettPå}")
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