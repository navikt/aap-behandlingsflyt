package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.Kvoter
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.komponenter.type.Periode

enum class Kvote(val avslagsårsak: VarighetVurdering.Avslagsårsak, val tellerMotKvote: (Vurdering) -> Boolean) {
    ORDINÆR(VarighetVurdering.Avslagsårsak.ORDINÆRKVOTE_BRUKT_OPP, ::skalTelleMotOrdinærKvote),
    SYKEPENGEERSTATNING(
        VarighetVurdering.Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP,
        ::skalTelleMotSykepengeKvote
    ),
    ;
}

private fun skalTelleMotOrdinærKvote(vurdering: Vurdering): Boolean {
    return vurdering.harRett() && vurdering.preliminærRettighetsType() in setOf(
        RettighetsType.BISTANDSBEHOV,
        RettighetsType.STUDENT
    ) && !skalTelleMotSykepengeKvote(vurdering)
}

private fun skalTelleMotSykepengeKvote(vurdering: Vurdering): Boolean {
    return vurdering.harRett() && vurdering.preliminærRettighetsType() == RettighetsType.SYKEPENGEERSTATNING
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
    private val sykepengeerstatningkvote: KvoteTilstand
) {
    constructor(kvoter: Kvoter) : this(
        ordinærkvote = KvoteTilstand(
            kvote = Kvote.ORDINÆR,
            hverdagerTilgjengelig = kvoter.ordinærkvote,
        ),
        sykepengeerstatningkvote = KvoteTilstand(
            kvote = Kvote.SYKEPENGEERSTATNING,
            hverdagerTilgjengelig = kvoter.sykepengeerstatningkvote
        )
    )

    private fun <T> map(relevanteKvoter: Set<Kvote>, action: (KvoteTilstand) -> T): List<T> {
        return listOfNotNull(
            if (Kvote.ORDINÆR in relevanteKvoter) action(ordinærkvote) else null,
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