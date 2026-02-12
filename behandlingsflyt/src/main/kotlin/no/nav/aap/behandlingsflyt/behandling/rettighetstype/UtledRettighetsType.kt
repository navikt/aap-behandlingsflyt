package no.nav.aap.behandlingsflyt.behandling.rettighetstype

import no.nav.aap.behandlingsflyt.behandling.underveis.KvoteService
import no.nav.aap.behandlingsflyt.behandling.underveis.Kvoter
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.StansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.tidslinje.tidslinjeOfNotNullPeriode
import no.nav.aap.komponenter.type.Periode
import java.time.DayOfWeek
import java.time.LocalDate


data class RettighetstypeVurdering(
    /** Er `null` hvis medlemmet ikke har rett etter noen av spesifikasjonenen. */
    val kravspesifikasjonForRettighetsType: KravspesifikasjonForRettighetsType?,
    val vilkårsvurderinger: Map<Vilkårtype, Vilkårsvurdering>,
)

internal data class Telleverk(
    val ordinærForbruk: Hverdager = Hverdager(0),
    val sykepengeerstatningForbruk: Hverdager = Hverdager(0),
) {
    fun maksdato(kvoter: Kvoter, kvote: Kvote, fom: LocalDate): LocalDate? {
        val hverdagerIgjen =
            when (kvote) {
                Kvote.ORDINÆR -> kvoter.ordinærkvote - ordinærForbruk
                Kvote.SYKEPENGEERSTATNING -> kvoter.sykepengeerstatningkvote - sykepengeerstatningForbruk
            }

        return when {
            Hverdager(0) < hverdagerIgjen ->
                hverdagerIgjen.fraOgMed(fom)

            hverdagerIgjen == Hverdager(0) && fom.dayOfWeek == DayOfWeek.SATURDAY ->
                fom.plusDays(1)

            hverdagerIgjen == Hverdager(0) && fom.dayOfWeek == DayOfWeek.SUNDAY ->
                fom

            else ->
                null
        }
    }

    fun oppdater(kvote: Kvote, hverdager: Hverdager): Telleverk {
        return when (kvote) {
            Kvote.ORDINÆR -> this.copy(ordinærForbruk = ordinærForbruk + hverdager)
            Kvote.SYKEPENGEERSTATNING -> this.copy(sykepengeerstatningForbruk = sykepengeerstatningForbruk + hverdager)
        }
    }
}

/** Her er det en del rom for forbedringer:
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
            ).komprimer()
        }
}

sealed interface KvoteVurdering {
    val rettighetstypeVurdering: RettighetstypeVurdering
    fun avslagsårsaker(): Set<Avslagsårsak>
    fun brukerAvKvoter(): Set<Kvote>

    val rettighetsType: RettighetsType?
        get() = rettighetstypeVurdering.kravspesifikasjonForRettighetsType?.rettighetstype
}

data class KvoteOk(
    val brukerKvote: Kvote?,
    override val rettighetstypeVurdering: RettighetstypeVurdering,
) : KvoteVurdering {
    override fun avslagsårsaker() = setOf<Avslagsårsak>()
    override fun brukerAvKvoter() = setOfNotNull(brukerKvote)
}

data class KvoteBruktOpp(
    val kvoteBruktOpp: Kvote,
    override val rettighetstypeVurdering: RettighetstypeVurdering,
) : KvoteVurdering {
    override fun avslagsårsaker() = setOf(kvoteBruktOpp.nyAvslagsårsak)
    override fun brukerAvKvoter() = emptySet<Kvote>()
}

internal fun vurderKvoter(
    kvoter: Kvoter,
    rettighetsType: Tidslinje<RettighetstypeVurdering>
): Tidslinje<KvoteVurdering> {
    var telleverk = Telleverk()

    return rettighetsType.flatMap { (periode, rettighetstypevurdering) ->
        val kvote = rettighetstypevurdering.kravspesifikasjonForRettighetsType?.rettighetstype?.kvote
            ?: return@flatMap tidslinjeOf(
                periode to KvoteOk(null, rettighetstypevurdering),
            )

        val maksdato = telleverk.maksdato(kvoter, kvote, periode.fom)
            ?: return@flatMap tidslinjeOf(
                periode to KvoteBruktOpp(kvote, rettighetstypevurdering)
            )

        val maksdatoForPeriode = minOf(maksdato, periode.tom)

        val periodeKvoteOk = Periode.orNull(periode.fom, maksdatoForPeriode)
        val periodeKvoteBruktOpp = Periode.orNull(maksdatoForPeriode.plusDays(1), periode.tom)
        telleverk = telleverk.oppdater(kvote, periode.antallHverdager())

        tidslinjeOfNotNullPeriode(
            periodeKvoteOk to KvoteOk(kvote, rettighetstypevurdering),
            periodeKvoteBruktOpp to KvoteBruktOpp(kvote, rettighetstypevurdering),
        )
    }
}

fun vurderRettighetsType(
    vilkårsresultat: Vilkårsresultat,
    kvoter: Kvoter = KvoteService().beregn()
): Tidslinje<RettighetsType> {
    val vurderKvoter = vurderRettighetstypeOgKvoter(vilkårsresultat, kvoter)
    return vurderKvoter
        .mapNotNull { kvotevurdering -> kvotevurdering.rettighetsType }
        .komprimer()
}

fun vurderRettighetstypeOgKvoter(
    vilkårsresultat: Vilkårsresultat,
    kvoter: Kvoter
): Tidslinje<KvoteVurdering> {
    val rettighetstypevurderinger = utledRettighetstypevurderinger(vilkårsresultat)
    return vurderKvoter(kvoter, rettighetstypevurderinger)
}

/** Identifiserer hva som er avslagsårsakene som fører til at medlemmet mister retten
 * til AAP. Her ser vi på overgangen fra å ha rett til AAP en dag til å ikke ha rett til AAP den neste dagen.
 *
 * NB1. Funksjonen handler kun om stans og opphør, ikke om avslag.
 *
 * NB2. Det skjer ytterligere vilkårsvurdering i Underveissteget. Disse opplysningene er ikke tilgjengelig her,
 * så for å helt korrekt svar, må denne vurderingen brukes som et ledd i underveissteget. Ideelt sett hadde
 * vi greid å hente ut alle vilkårsvurderingen som fører til stans eller opphør ut fra underveissteget og inn
 * i egne vilkår, slik at vi ikke trenger å splitte utregningen i to.
 *
 * @return Map over hvilke avslagsårsaker som er grunnen til at medlemmet går fra å ha rett til AAP en dag til ikke å ha rett til AAP dagen umiddelbart etterpå.
 * Dette er egentlig ikke en egenskap knyttet til en dag, men til overgangen mellom to dager. Avslagsårsakene vil derfor ikke være oppgitt for perioden uten rett, men for
 * første dag uten rett.
 *
 * I perioden etter en stans eller et opphør kan det være varierende grunner til at medlemmet ikke har rett til AAP. Når disse varierer etter stans eller opphør,
 * så er ikke dette nye opphør eller stans – for meldemmet har ikke AAP å stanse eller opphøre.
 */
fun utledStansEllerOpphør(
    vilkårsresultat: Vilkårsresultat,
    kvoter: Kvoter = KvoteService().beregn(),
): Map<LocalDate, StansEllerOpphør> {
    val rettighetstypeVurderingTidslinje = utledRettighetstypevurderinger(vilkårsresultat)
        .let {
            /* Fyll "tomrom" i tidslinjen. Body til [windowed]-kallet forutsetter at segmenter
             * ligger inntil hverandre. */
            it.mergePrioriterVenstre(
                tidslinjeOf(
                    it.helePerioden() to RettighetstypeVurdering(
                        kravspesifikasjonForRettighetsType = null,
                        vilkårsvurderinger = emptyMap(),
                    )
                )
            )
        }
    return vurderKvoter(kvoter, rettighetstypeVurderingTidslinje)
        .segmenter().windowed(2)
        .mapNotNull { (vurderingSegment, nesteVurderingSegment) ->
            require(vurderingSegment.tom().plusDays(1) == nesteVurderingSegment.fom()) {
                """Korrektheten av koden under er avhengig av en sammenhengende tidslinje
                    |for å oppdage perioder uten AAP.
                """.trimMargin()
            }
            val (_, kvotevurdering) = vurderingSegment
            val (_, nesteKvotevurdering) = nesteVurderingSegment

            val innvilgendeKravspesifikasjon = kvotevurdering.rettighetstypeVurdering.kravspesifikasjonForRettighetsType
            val nesteInnvilgendeKravspesifikasjon =
                nesteKvotevurdering.rettighetstypeVurdering.kravspesifikasjonForRettighetsType
            val haddeRettTilAAP = innvilgendeKravspesifikasjon != null && kvotevurdering is KvoteOk
            val misterRettTilAAP = nesteInnvilgendeKravspesifikasjon == null || nesteKvotevurdering is KvoteBruktOpp
            if (haddeRettTilAAP && misterRettTilAAP) {
                val sisteDagMedRett = vurderingSegment.periode.tom
                val avslagsårsaker =
                    innvilgendeKravspesifikasjon.avslagsårsaker(nesteKvotevurdering.rettighetstypeVurdering.vilkårsvurderinger) +
                            nesteKvotevurdering.avslagsårsaker()
                sisteDagMedRett.plusDays(1) to StansEllerOpphør.fraÅrsaker(avslagsårsaker)
            } else {
                null
            }
        }
        .toMap()
}

