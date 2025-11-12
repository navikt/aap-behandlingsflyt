package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.Kvoter

private data class KvoteTilstand(
    val kvote: Kvote,
    private val hverdagerTilgjengelig: Hverdager,
    private var hverdagerBrukt: Hverdager = Hverdager(0),
) {
    var erKvoteOversteget: Boolean = false
        set(kvoteErOversteget) {
            if (kvoteErOversteget) {
                field = true
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
            ?.value.orEmpty()
            .map { it.kvote }
            .toSet()
    }

    fun øk(relevanteKvoter: Set<Kvote>, hverdagerBrukt: Hverdager) {
        map(relevanteKvoter) {
            it.øk(hverdagerBrukt)
        }
    }

    private fun <T> map(relevanteKvoter: Set<Kvote>, action: (KvoteTilstand) -> T): List<T> {
        return listOfNotNull(
            if (Kvote.ORDINÆR in relevanteKvoter) action(ordinærkvote) else null,
            if (Kvote.STUDENT in relevanteKvoter) action(studentkvote) else null,
            if (Kvote.ETABLERINGSFASE in relevanteKvoter) action(etableringsfasekvote) else null,
            if (Kvote.UTVIKLINGSFASE in relevanteKvoter) action(utviklingsfasekvote) else null,
            if (Kvote.SYKEPENGEERSTATNING in relevanteKvoter) action(sykepengeerstatningkvote) else null,
        )
    }
}