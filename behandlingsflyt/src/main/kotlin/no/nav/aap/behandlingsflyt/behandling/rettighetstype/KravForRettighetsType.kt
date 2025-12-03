package no.nav.aap.behandlingsflyt.behandling.rettighetstype

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KravspesifikasjonForRettighetsType.IngenKrav
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KravspesifikasjonForRettighetsType.IngenKravOmForutgåendeAAP
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KravspesifikasjonForRettighetsType.KravOmForutgåendeAAP
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KravspesifikasjonForRettighetsType.MåVæreOppfylt
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode

private val kravprioritet =
    /* Rekkefølgen på disse er av betydning: første match blir valgt.  Ideelt sett burde de nok være i samme rekkefølge som vi vurdere vilkår i flyten. Samtidig er nok
    * en del (nesten alle?) av kravene gjensidig utelukkende. */
    listOf(
        KravForStudent,
        KravForOrdinærAap,
        KravForYrkesskade,
        KravForOvergangUføretrygd,
        KravForSykepengeerstatning,
        KravForSykepengeerstatningGammeltFormat,
        KravForOvergangArbeid,
    )


data class RettighetstypeVurdering(
    /** Er `null` hvis medlemmet ikke har rett etter noen av spesifikasjonenen. */
    val kravspesifikasjonForRettighetsType: KravspesifikasjonForRettighetsType?,
    val vilkårsvurderinger: Map<Vilkårtype, Vilkårsvurdering>,
)

/* her er det en del rom for forbedringer:
 * 1. regn ut alle rettighetstyper som er mulige, ikke bare en
 *
 * 2. bygg opp en konkret begrunnelse:
 *      - oppfylt, ikke oppfylt: liste med nøyaktig hvilke vilkår ble vurdert
 *      - ikke vurdert: liste med hvilke delvilkår som mangler vurdering
 *
 * 3. utvid utfall til: ikke oppfylt, oppfylt, ukjent
 *      - Dette kan da brukes til å forbedre TidligereVurdering: så lenge det finnes
 *        en vilkårstype som er ukjent eller oppfylt, så er ikke avslag garantert.
 */
fun utledRettighetstypevurderinger(vilkårsresultat: Vilkårsresultat): Tidslinje<RettighetstypeVurdering> {
    return vilkårsresultat.somTidslinje()
        .fold(Tidslinje<RettighetstypeVurdering>()) { forutgåendeTidslinje, periode, vilkårsvurderinger ->
            /* NB: Vi har ikke noen mekanismer for å identifisere opphør. Når vi får opphør, så skal [forutgåendeTidslinje]
            *   begrenses til perioden etter opphør når den sendes inn til oppfylles av. */
            val forutgåendeRettighetstyper =
                forutgåendeTidslinje.mapNotNull { it.kravspesifikasjonForRettighetsType?.rettighetstype }

            val kravspesifikasjon = kravprioritet.firstOrNull { kravspesifikasjon ->
                kravspesifikasjon.oppfyllesAv(forutgåendeRettighetstyper, vilkårsvurderinger)
            }

            forutgåendeTidslinje.mergePrioriterHøyre(
                tidslinjeOf(
                    periode to RettighetstypeVurdering(
                        kravspesifikasjonForRettighetsType = kravspesifikasjon,
                        vilkårsvurderinger = vilkårsvurderinger
                    )
                )
            )
        }
        .segmenter()
        .map { segment -> Segment(segment.periode, segment.verdi) }
        .let(::Tidslinje)
        .komprimer()
}

object KravForSykepengeerstatningGammeltFormat : KravspesifikasjonForRettighetsType {
    override val rettighetstype = RettighetsType.SYKEPENGEERSTATNING

    override val kravForutgåendeMedlemskap = MåVæreOppfylt()
    override val kravSykdom = MåVæreOppfylt(Innvilgelsesårsak.SYKEPENGEERSTATNING)

    override val kravSykepengeerstatning = IngenKrav
    override val kravBistand = IngenKrav
    override val kravOvergangUfør = IngenKrav
    override val kravOvergangArbeid = IngenKrav
    override val forutgåendeAap = IngenKravOmForutgåendeAAP
}

data object KravForStudent : KravspesifikasjonForRettighetsType {
    override val rettighetstype = RettighetsType.STUDENT
    override val kravForutgåendeMedlemskap = MåVæreOppfylt()
    override val kravSykdom = MåVæreOppfylt(Innvilgelsesårsak.STUDENT)
    override val kravBistand = IngenKrav

    override val kravOvergangUfør = IngenKrav
    override val kravOvergangArbeid = IngenKrav
    override val kravSykepengeerstatning = IngenKrav
    override val forutgåendeAap = IngenKravOmForutgåendeAAP
}

data object KravForOrdinærAap : KravspesifikasjonForRettighetsType {
    override val rettighetstype = RettighetsType.BISTANDSBEHOV

    override val kravForutgåendeMedlemskap = MåVæreOppfylt()
    override val kravSykdom = MåVæreOppfylt()
    override val kravBistand = MåVæreOppfylt()

    override val kravOvergangUfør = IngenKrav
    override val kravOvergangArbeid = IngenKrav
    override val kravSykepengeerstatning = IngenKrav
    override val forutgåendeAap = IngenKravOmForutgåendeAAP
}

data object KravForYrkesskade : KravspesifikasjonForRettighetsType {
    override val rettighetstype = RettighetsType.BISTANDSBEHOV

    override val kravSykdom = MåVæreOppfylt(Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG)
    override val kravBistand = MåVæreOppfylt()

    override val kravForutgåendeMedlemskap = IngenKrav
    override val kravOvergangUfør = IngenKrav
    override val kravOvergangArbeid = IngenKrav
    override val kravSykepengeerstatning = IngenKrav
    override val forutgåendeAap = IngenKravOmForutgåendeAAP
}

data object KravForSykepengeerstatning : KravspesifikasjonForRettighetsType {
    override val rettighetstype = RettighetsType.SYKEPENGEERSTATNING

    override val kravForutgåendeMedlemskap = MåVæreOppfylt()
    override val kravSykepengeerstatning = MåVæreOppfylt()

    override val kravSykdom = IngenKrav
    override val kravBistand = IngenKrav
    override val kravOvergangUfør = IngenKrav
    override val kravOvergangArbeid = IngenKrav
    override val forutgåendeAap = IngenKravOmForutgåendeAAP
}

data object KravForOvergangUføretrygd : KravspesifikasjonForRettighetsType {
    override val rettighetstype = RettighetsType.VURDERES_FOR_UFØRETRYGD

    override val kravForutgåendeMedlemskap = MåVæreOppfylt()
    override val kravSykdom = IngenKrav
    override val kravOvergangUfør = MåVæreOppfylt(null, Innvilgelsesårsak.VURDERES_FOR_UFØRETRYGD)

    override val kravBistand = IngenKrav
    override val kravOvergangArbeid = IngenKrav
    override val kravSykepengeerstatning = IngenKrav
    override val forutgåendeAap = IngenKravOmForutgåendeAAP
}

data object KravForOvergangArbeid : KravspesifikasjonForRettighetsType {
    override val rettighetstype = RettighetsType.ARBEIDSSØKER

    override val kravForutgåendeMedlemskap = MåVæreOppfylt()
    override val kravOvergangArbeid = MåVæreOppfylt()
    override val forutgåendeAap = KravOmForutgåendeAAP(RettighetsType.BISTANDSBEHOV)

    override val kravBistand = IngenKrav
    override val kravSykdom = IngenKrav
    override val kravOvergangUfør = IngenKrav
    override val kravSykepengeerstatning = IngenKrav
}

fun vurderRettighetsType(vilkårsresultat: Vilkårsresultat): Tidslinje<RettighetsType> {
    return utledRettighetstypevurderinger(vilkårsresultat).mapNotNull { it.kravspesifikasjonForRettighetsType?.rettighetstype }
}

/** Identifiserer hva som er avslagsårsakene som fører til at medlemmet mister retten
 * til AAP. Her ser vi på overgangen fra å ha rett til AAP en dag til å ikke ha rett til AAP den neste dagen.
 *
 * NB. funksjonen sier ikke hvorfor vi ikke innvilger AAP.
 *
 * NB2. Det skjer ytterligere vilkårsvurdering i Underveissteget. Disse opplysningene er ikke tilgjengelig her,
 * så for å helt korrekt svar, må denne vurderingen brukes som et ledd i underveissteget. Ideelt sett hadde
 * vi greid å hente ut alle vilkårsvurderingen som fører til stans eller opphør ut fra underveissteget og inn
 * i egne vilkår, slik at vi ikke trenger å splitte utregningen i to.
 *
 * @return Tidslinjen forteller hvilke(t) avslagsårsak(er) som er grunnen til at medlemmet går fra å ha rett til AAP en dag til ikke å ha rett til AAP dagen umiddelbart etterpå.
 * Dette er egentlig ikke en egenskap knyttet til en dag, men til overgangen mellom to dager. Avslagsårsakene vil derfor ikke være oppgitt for perioden uten rett, men for
 * siste dag med rett.
 *
 * I perioden etter en stans eller et opphør kan det være varierende grunner til at medlemmet ikke har rett til AAP. Når disse varierer etter stans eller opphør,
 * så er ikke dette nye opphør eller stans – for meldemmet har ikke AAP å stanse eller opphøre.
 */
fun avslagsårsakerVedTapAvRettPåAAP(
    vilkårsresultat: Vilkårsresultat
): Tidslinje<Set<Avslagsårsak>> {
    val rettighetstypeVurderingTidslinje = utledRettighetstypevurderinger(vilkårsresultat)
    return rettighetstypeVurderingTidslinje
        .mergePrioriterVenstre(
            tidslinjeOf(
                rettighetstypeVurderingTidslinje.helePerioden() to RettighetstypeVurdering(
                    null,
                    emptyMap()
                )
            )
        )
        .segmenter().windowed(2)
        .flatMap { (vurderingSegment, nesteVurderingSegment) ->
            val innvilgendeKravspesifikasjon = vurderingSegment.verdi.kravspesifikasjonForRettighetsType
            val nesteInnvilgendeKravspesifikasjon =
                nesteVurderingSegment.verdi.kravspesifikasjonForRettighetsType
            if (innvilgendeKravspesifikasjon != null && nesteInnvilgendeKravspesifikasjon == null) {
                /* hadde rett rett, men mister den */
                val sisteDagMedRett = vurderingSegment.periode.tom
                val avslagsårsaker =
                    innvilgendeKravspesifikasjon.avslagsårsaker(nesteVurderingSegment.verdi.vilkårsvurderinger)
                listOf(Segment(Periode(sisteDagMedRett, sisteDagMedRett), avslagsårsaker))
            } else {
                listOf()
            }
        }
        .let(::Tidslinje)
}

fun <T> Tidslinje<T>.mergePrioriterVenstre(other: Tidslinje<T>): Tidslinje<T> {
    return other.mergePrioriterHøyre(this)
}

