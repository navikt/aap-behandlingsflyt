package no.nav.aap.behandlingsflyt.behandling.rettighetstype

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KravspesifikasjonForRettighetsType.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.outerJoinNotNull
import no.nav.aap.komponenter.tidslinje.tidslinjeOf


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
fun vurderRettighetsType(vilkårsresultat: Vilkårsresultat): Tidslinje<RettighetsType> {
    return vilkårsresultat
        .alle()
        .map { vilkår -> vilkår.tidslinje().mapValue { vurdering -> vilkår.type to vurdering } }
        .outerJoinNotNull { it.toMap() }
        .fold(Tidslinje<RettighetsType>()) { forutgåendeTidslinje, periode, vilkårsvurderinger ->
            /* NB: Vi har ikke noen mekanismer for å identifisere opphør. Når vi får opphør, så skal [forutgåendeTidslinje]
            *   begrenses til perioden etter opphør når den sendes inn til oppfylles av. */
            val rettighetsType = when {
                /* Prioritert rekkefølge */
                KravForStudent.oppfyllesAv(forutgåendeTidslinje, vilkårsvurderinger) -> RettighetsType.STUDENT
                KravForOvergangUføretrygd.oppfyllesAv(forutgåendeTidslinje, vilkårsvurderinger) -> RettighetsType.VURDERES_FOR_UFØRETRYGD
                KravForSykepengeerstatning.oppfyllesAv(forutgåendeTidslinje, vilkårsvurderinger) -> RettighetsType.SYKEPENGEERSTATNING
                KravForOvergangArbeid.oppfyllesAv(forutgåendeTidslinje, vilkårsvurderinger) -> RettighetsType.ARBEIDSSØKER
                KravForOrdinærAap.oppfyllesAv(forutgåendeTidslinje, vilkårsvurderinger) -> RettighetsType.BISTANDSBEHOV
                KravForYrkesskade.oppfyllesAv(forutgåendeTidslinje, vilkårsvurderinger) -> RettighetsType.BISTANDSBEHOV
                else -> null
            }

            if (rettighetsType == null)
                forutgåendeTidslinje
            else
                forutgåendeTidslinje.mergePrioriterHøyre(tidslinjeOf(periode to rettighetsType))
        }
        .segmenter()
        .map { segment -> Segment(segment.periode, segment.verdi) }
        .let(::Tidslinje)
        .komprimer()
}

object KravForStudent : KravspesifikasjonForRettighetsType {
    override val kravForutgåendeMedlemskap = MåVæreOppfylt()
    override val kravSykdom = MåVæreOppfylt(Innvilgelsesårsak.STUDENT)
    override val kravBistand = MåVæreOppfylt(Innvilgelsesårsak.STUDENT)

    override val kravOvergangUfør = IngenKrav
    override val kravOvergangArbeid = IngenKrav
    override val forutgåendeAap = IngenKravOmForutgåendeAAP
}

object KravForOrdinærAap : KravspesifikasjonForRettighetsType {
    override val kravForutgåendeMedlemskap = MåVæreOppfylt()
    override val kravSykdom = MåVæreOppfylt()
    override val kravBistand = MåVæreOppfylt()

    override val kravOvergangUfør = IngenKrav
    override val kravOvergangArbeid = IngenKrav
    override val forutgåendeAap = IngenKravOmForutgåendeAAP
}

object KravForYrkesskade: KravspesifikasjonForRettighetsType {
    override val kravSykdom = MåVæreOppfylt(Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG)
    override val kravBistand = MåVæreOppfylt()

    override val kravForutgåendeMedlemskap = IngenKrav
    override val kravOvergangUfør = IngenKrav
    override val kravOvergangArbeid = IngenKrav
    override val forutgåendeAap = IngenKravOmForutgåendeAAP
}

object KravForSykepengeerstatning : KravspesifikasjonForRettighetsType {
    override val kravForutgåendeMedlemskap = MåVæreOppfylt()
    override val kravSykdom = MåVæreOppfylt(Innvilgelsesårsak.SYKEPENGEERSTATNING)

    override val kravBistand = IngenKrav
    override val kravOvergangUfør = IngenKrav
    override val kravOvergangArbeid = IngenKrav
    override val forutgåendeAap = IngenKravOmForutgåendeAAP
}

object KravForOvergangUføretrygd : KravspesifikasjonForRettighetsType {
    override val kravForutgåendeMedlemskap = MåVæreOppfylt()
    override val kravSykdom = IngenKrav
    override val kravOvergangUfør = MåVæreOppfylt(null, Innvilgelsesårsak.VURDERES_FOR_UFØRETRYGD)

    override val kravBistand = IngenKrav
    override val kravOvergangArbeid = IngenKrav
    override val forutgåendeAap = IngenKravOmForutgåendeAAP
}

object KravForOvergangArbeid : KravspesifikasjonForRettighetsType {
    override val kravForutgåendeMedlemskap = MåVæreOppfylt()
    override val kravOvergangArbeid = MåVæreOppfylt()
    override val forutgåendeAap = KravOmForutgåendeAAP(RettighetsType.BISTANDSBEHOV)

    override val kravBistand = IngenKrav
    override val kravSykdom = IngenKrav
    override val kravOvergangUfør = IngenKrav
}
