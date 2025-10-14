package no.nav.aap.behandlingsflyt.behandling.rettighetstype

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KravspesifikasjonForRettighetsType.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.outerJoinNotNull


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
        .mapValue {
            when {
                /* Prioritert rekkefølge */
                KravForStudent.oppfyllesAv(it) -> RettighetsType.STUDENT
                KravForOvergangUføretrygd.oppfyllesAv(it) -> RettighetsType.VURDERES_FOR_UFØRETRYGD
                KravForSykepengeerstatning.oppfyllesAv(it) -> RettighetsType.SYKEPENGEERSTATNING
                KravForOvergangArbeid.oppfyllesAv(it) -> RettighetsType.ARBEIDSSØKER
                KravForOrdinærAap.oppfyllesAv(it) -> RettighetsType.BISTANDSBEHOV
                KravForYrkesskade.oppfyllesAv(it) -> RettighetsType.BISTANDSBEHOV
                else -> null
            }
        }
        .segmenter()
        .mapNotNull { segment -> segment.verdi?.let { Segment(segment.periode, it) } }
        .let(::Tidslinje)
        .komprimer()
}

object KravForStudent : KravspesifikasjonForRettighetsType {
    override val kravForutgåendeMedlemskap = MåVæreOppfylt()
    override val kravSykdom = MåVæreOppfylt(Innvilgelsesårsak.STUDENT)
    override val kravBistand = MåVæreOppfylt(Innvilgelsesårsak.STUDENT)

    override val kravOvergangUfør = IngenKrav
    override val kravOvergangArbeid = IngenKrav
}

object KravForOrdinærAap : KravspesifikasjonForRettighetsType {
    override val kravForutgåendeMedlemskap = MåVæreOppfylt()
    override val kravSykdom = MåVæreOppfylt()
    override val kravBistand = MåVæreOppfylt()

    override val kravOvergangUfør = IngenKrav
    override val kravOvergangArbeid = IngenKrav
}

object KravForYrkesskade: KravspesifikasjonForRettighetsType {
    override val kravSykdom = MåVæreOppfylt(Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG)
    override val kravBistand = MåVæreOppfylt()

    override val kravForutgåendeMedlemskap = IngenKrav
    override val kravOvergangUfør = IngenKrav
    override val kravOvergangArbeid = IngenKrav
}

object KravForSykepengeerstatning : KravspesifikasjonForRettighetsType {
    override val kravForutgåendeMedlemskap = MåVæreOppfylt()
    override val kravSykdom = MåVæreOppfylt(Innvilgelsesårsak.SYKEPENGEERSTATNING)

    override val kravBistand = IngenKrav
    override val kravOvergangUfør = IngenKrav
    override val kravOvergangArbeid = IngenKrav
}

object KravForOvergangUføretrygd : KravspesifikasjonForRettighetsType {
    override val kravForutgåendeMedlemskap = MåVæreOppfylt()
    override val kravSykdom = IngenKrav
    override val kravOvergangUfør = MåVæreOppfylt(null, Innvilgelsesårsak.VURDERES_FOR_UFØRETRYGD)

    override val kravBistand = IngenKrav
    override val kravOvergangArbeid = IngenKrav
}

object KravForOvergangArbeid : KravspesifikasjonForRettighetsType {
    override val kravForutgåendeMedlemskap = MåVæreOppfylt()
    override val kravOvergangArbeid = MåVæreOppfylt(null, Innvilgelsesårsak.ARBEIDSSØKER)

    override val kravBistand = IngenKrav
    override val kravSykdom = IngenKrav
    override val kravOvergangUfør = IngenKrav
}
