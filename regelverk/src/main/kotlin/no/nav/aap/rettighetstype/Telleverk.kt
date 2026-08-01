package no.nav.aap.rettighetstype

import java.time.DayOfWeek
import java.time.LocalDate
import no.nav.aap.underveis.Hverdager
import no.nav.aap.kvote.Kvote
import no.nav.aap.kvote.Kvoter
import no.nav.aap.vilkårsresultat.Avslagsårsak
import no.nav.aap.vilkårsresultat.RettighetsType
import no.nav.aap.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.vilkårsresultat.Vilkårtype

data class RettighetstypeVurdering(
    /** Er `null` hvis medlemmet ikke har rett etter noen av spesifikasjonenen. */
    val kravspesifikasjonForRettighetsType: KravspesifikasjonForRettighetsType?,
    val vilkårsvurderinger: Map<Vilkårtype, Vilkårsvurdering>,
)

data class Telleverk(
    val ordinærForbruk: Hverdager = Hverdager(0),
    val sykepengeerstatningForbruk: Hverdager = Hverdager(0),
) {
    fun maksdato(kvoter: Kvoter, kvote: Kvote, fom: LocalDate, forrigeKvoteVurdering: KvoteVurdering?): LocalDate? {
        val hverdagerIgjen =
            when (kvote) {
                Kvote.ORDINÆR -> kvoter.ordinærkvote - ordinærForbruk
                Kvote.SYKEPENGEERSTATNING -> kvoter.sykepengeerstatningkvote - sykepengeerstatningForbruk
            }

        return when {
            Hverdager(0) < hverdagerIgjen ->
                hverdagerIgjen.fraOgMed(fom)

            hverdagerIgjen == Hverdager(0) && fom.dayOfWeek == DayOfWeek.SATURDAY  && forrigeKvoteVurdering is KvoteOk ->
                fom.plusDays(1)

            hverdagerIgjen == Hverdager(0) && fom.dayOfWeek == DayOfWeek.SUNDAY && forrigeKvoteVurdering is KvoteOk ->
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
    override fun avslagsårsaker() = setOf(kvoteBruktOpp.avslagsårsak)
    override fun brukerAvKvoter() = emptySet<Kvote>()
}