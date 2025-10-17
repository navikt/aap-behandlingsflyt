package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.Kvoter
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.komponenter.type.Periode
import java.time.DayOfWeek
import java.time.LocalDate

@JvmInline
value class Hverdager(val asInt: Int) : Comparable<Hverdager> {
    operator fun plus(other: Hverdager) = Hverdager(this.asInt + other.asInt)
    operator fun minus(other: Hverdager) = Hverdager(this.asInt - other.asInt)
    override fun compareTo(other: Hverdager) = asInt.compareTo(other.asInt)

    companion object {
        fun LocalDate.plusHverdager(hverdager: Hverdager): LocalDate {
            return hverdagerFraOgMed(this).elementAt(hverdager.asInt)
        }

        private val hverdagene = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )

        private val LocalDate.erHverdag: Boolean
            get() = dayOfWeek in hverdagene

        fun Periode.antallHverdager(): Hverdager {
            return Hverdager(this.antallDager(*hverdagene.toTypedArray()))
        }

        private fun hverdagerFraOgMed(start: LocalDate): Sequence<LocalDate> {
            var dag = start
            return sequence {
                while (true) {
                    if (dag.erHverdag) {
                        yield(dag)
                    }
                    dag = dag.plusDays(1)
                }
            }
        }
    }
}

enum class Kvote(val avslagsårsak: VarighetVurdering.Avslagsårsak, val tellerMotKvote: (Vurdering) -> Boolean) {
    ORDINÆR(VarighetVurdering.Avslagsårsak.ORDINÆRKVOTE_BRUKT_OPP, ::skalTelleMotOrdinærKvote),
    STUDENT(VarighetVurdering.Avslagsårsak.STUDENTKVOTE_BRUKT_OPP, ::skalTelleMotStudentKvote),
    ETABLERINGSFASE(VarighetVurdering.Avslagsårsak.ETABLERINGSFASEKVOTE_BRUKT_OPP, { false }),
    UTVIKLINGSFASE(VarighetVurdering.Avslagsårsak.UTVIKLINGSFASEKVOTE_BRUKT_OPP, { false }),
    SYKEPENGEERSTATNING(
        VarighetVurdering.Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP,
        ::skalTelleMotSykepengeKvote
    );
}

private fun skalTelleMotOrdinærKvote(vurdering: Vurdering): Boolean {
    return vurdering.harRett() && vurdering.rettighetsType() in setOf(
        RettighetsType.BISTANDSBEHOV,
        RettighetsType.STUDENT
    ) && !skalTelleMotSykepengeKvote(vurdering)
}

private fun skalTelleMotStudentKvote(vurdering: Vurdering): Boolean {
    return vurdering.harRett() && vurdering.rettighetsType() == RettighetsType.STUDENT
}

private fun skalTelleMotSykepengeKvote(vurdering: Vurdering): Boolean {
    return vurdering.harRett() && vurdering.rettighetsType() == RettighetsType.SYKEPENGEERSTATNING
}


data class KvoteTilstand(
    val kvote: Kvote,
    private val hverdagerTilgjengelig: Hverdager,
    private var hverdagerBrukt: Hverdager = Hverdager(0),
) {
    var erKvoteOversteget: Boolean = false
        set(kvoteErOversteget) {
            if (kvoteErOversteget) {
                field = kvoteErOversteget
            }
        }

    val ubruktKvote: Hverdager
        get() = hverdagerTilgjengelig - hverdagerBrukt

    fun øk(brukt: Hverdager) {
        hverdagerBrukt += brukt
    }
}

class Telleverk private constructor(
    private val ordinærkvote: KvoteTilstand,
    private val studentkvote: KvoteTilstand,
    private val utviklingsfasekvote: KvoteTilstand,
    private val etableringsfasekvote: KvoteTilstand,
    private val sykepengeerstatningkvote: KvoteTilstand
) {
    constructor(kvoter: Kvoter) : this(
        ordinærkvote = KvoteTilstand(
            kvote = Kvote.ORDINÆR,
            hverdagerTilgjengelig = kvoter.ordinærkvote,
        ),
        studentkvote = KvoteTilstand(
            kvote = Kvote.STUDENT,
            hverdagerTilgjengelig = kvoter.studentkvote,
        ),
        utviklingsfasekvote = KvoteTilstand(
            kvote = Kvote.UTVIKLINGSFASE,
            hverdagerTilgjengelig = Hverdager(0),
        ),
        etableringsfasekvote = KvoteTilstand(
            kvote = Kvote.ETABLERINGSFASE,
            hverdagerTilgjengelig = Hverdager(0),
        ),
        sykepengeerstatningkvote = KvoteTilstand(
            kvote = Kvote.SYKEPENGEERSTATNING,
            hverdagerTilgjengelig = kvoter.sykepengeerstatningkvote
        )
    )

    private fun <T> map(relevanteKvoter: Set<Kvote>, action: (KvoteTilstand) -> T): List<T> {
        return listOfNotNull(
            if (Kvote.ORDINÆR in relevanteKvoter) action(ordinærkvote) else null,
            if (Kvote.STUDENT in relevanteKvoter) action(studentkvote) else null,
            if (Kvote.ETABLERINGSFASE in relevanteKvoter) action(etableringsfasekvote) else null,
            if (Kvote.UTVIKLINGSFASE in relevanteKvoter) action(utviklingsfasekvote) else null,
            if (Kvote.SYKEPENGEERSTATNING in relevanteKvoter) action(sykepengeerstatningkvote) else null,
        )
    }

    fun markereKvoterOversteget(kvoterSomSkalMarkeres: Set<Kvote>) {
        map(kvoterSomSkalMarkeres) {
            it.erKvoteOversteget = true
        }
    }

    fun erKvoterStanset(relevanteKvoter: Set<Kvote>): Boolean {
        return map(relevanteKvoter) { it.erKvoteOversteget }.any { it }
    }

    fun minsteUbrukteKvote(relevanteKvoter: Set<Kvote>): Hverdager {
        return map(relevanteKvoter) { it.ubruktKvote }.min()
    }

    fun kvoterNærmestÅBliBruktOpp(relevanteKvoter: Set<Kvote>): Set<Kvote> {
        return map(relevanteKvoter) { it }
            .groupBy { it.ubruktKvote }
            .minByOrNull { it.key }
            ?.value
            ?.map { it.kvote }
            ?.toSet().orEmpty()
    }

    fun øk(relevanteKvoter: Set<Kvote>, hverdagerBrukt: Hverdager) {
        map(relevanteKvoter) {
            it.øk(hverdagerBrukt)
        }
    }

    fun øk(relevanteKvoter: Set<Kvote>, periode: Periode) {
        øk(relevanteKvoter, periode.antallHverdager())
    }
}