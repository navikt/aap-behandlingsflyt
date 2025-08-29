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
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
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
                        fom = vurdering.vurderingenGjelderFra ?: grunnlag.kravDato,
                        tom = grunnlag.sisteDagMedMuligYtelse
                    ),
                    vurdering
                )
            }
            .fold(Tidslinje<Sykdomsvurdering>()) { t1, t2 ->
                t1.kombiner(t2, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            }

        val sykepengerVurdering = grunnlag.sykepengerErstatningFaktagrunnlag

        val tidslinje =
            Tidslinje.zip3(
                studentVurderingTidslinje,
                yrkesskadeVurderingTidslinje,
                sykdomsvurderingTidslinje,
            )
                .mapValue { (studentVurdering, yrkesskadeVurdering, sykdomVurdering) ->
                    opprettVilkårsvurdering(
                        studentVurdering,
                        sykdomVurdering,
                        yrkesskadeVurdering,
                        sykepengerVurdering,
                        grunnlag
                    )
                }

        vilkår.leggTilVurderinger(tidslinje)
    }

    private fun opprettVilkårsvurdering(
        studentVurdering: StudentVurdering?,
        sykdomVurdering: Sykdomsvurdering?,
        yrkesskadeVurdering: Yrkesskadevurdering?,
        sykepengerVurdering: SykepengerVurdering?,
        grunnlag: SykdomsFaktagrunnlag
    ): Vilkårsvurdering {
        var utfall: Utfall
        var avslagsårsak: Avslagsårsak? = null
        var innvilgelsesårsak: Innvilgelsesårsak? = null

        if (studentVurdering?.erOppfylt() == true) {
            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = Innvilgelsesårsak.STUDENT
        } else if (harSykdomBlittVurdertTilGodkjent(sykdomVurdering, yrkesskadeVurdering)) {
            utfall = Utfall.OPPFYLT
            if (yrkesskadeVurdering != null && yrkesskadeVurdering.erÅrsakssammenheng) {
                innvilgelsesårsak = Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG
            }
            if (sykepengerVurdering?.harRettPå == true) {
                innvilgelsesårsak = Innvilgelsesårsak.SYKEPENGEERSTATNING
            }
        } else {
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = if (sykdomVurdering?.erSkadeSykdomEllerLyteVesentligdel == false) {
                Avslagsårsak.IKKE_SYKDOM_SKADE_LYTE_VESENTLIGDEL
            } else if (sykdomVurdering?.erNedsettelseIArbeidsevneMerEnnHalvparten == false && sykdomVurdering.erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense != true) {
                Avslagsårsak.IKKE_NOK_REDUSERT_ARBEIDSEVNE
            } else if (sykdomVurdering?.erNedsettelseIArbeidsevneAvEnVissVarighet == false && relevantÅVurdereSykepengerErstatning(
                    grunnlag,
                    sykdomVurdering
                )
            ) {
                Avslagsårsak.IKKE_SYKDOM_AV_VISS_VARIGHET
            } else {
                // TODO: dekk alle muligheter for nei her
                Avslagsårsak.MANGLENDE_DOKUMENTASJON // TODO noe mer rett
            }

            log.info("Avslagsårsak $avslagsårsak. Er nedsettelse i arbeidsevne: ${sykdomVurdering?.erNedsettelseIArbeidsevneAvEnVissVarighet}. Har rett på sykepengeerstatning: ${sykepengerVurdering?.harRettPå}")

            if (sykepengerVurdering?.harRettPå == true && avslagsårsak == Avslagsårsak.IKKE_SYKDOM_AV_VISS_VARIGHET) {
                utfall = Utfall.OPPFYLT
                innvilgelsesårsak = Innvilgelsesårsak.SYKEPENGEERSTATNING
                avslagsårsak = null
            }
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

    private fun relevantÅVurdereSykepengerErstatning(
        grunnlag: SykdomsFaktagrunnlag,
        sykdomVurdering: Sykdomsvurdering?
    ): Boolean {
        return (grunnlag.kravDato == (sykdomVurdering?.vurderingenGjelderFra ?: grunnlag.kravDato))
                || (grunnlag.typeBehandling == TypeBehandling.Førstegangsbehandling)
    }

    private fun harSykdomBlittVurdertTilGodkjent(
        sykdomsvurdering: Sykdomsvurdering?,
        yrkesskadevurdering: Yrkesskadevurdering?
    ): Boolean {
        if (sykdomsvurdering == null) {
            return false
        }
        return sykdomsvurdering.run {
            erSkadeSykdomEllerLyteVesentligdel == true &&
                    erArbeidsevnenNedsatt == true && (erNedsettelseIArbeidsevneAvEnVissVarighet == null || erNedsettelseIArbeidsevneAvEnVissVarighet) && (erNedsettelseIArbeidsevneMerEnnHalvparten == true ||
                    (erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true && yrkesskadevurdering?.erÅrsakssammenheng == true))
        }
    }
}