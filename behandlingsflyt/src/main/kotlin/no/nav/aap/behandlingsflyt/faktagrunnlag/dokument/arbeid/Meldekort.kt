package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDateTime

data class Meldekort(
    val journalpostId: JournalpostId,
    val timerArbeidPerPeriode: Set<ArbeidIPeriode>,
    val mottattTidspunkt: LocalDateTime,
    val fravær: Set<FraværForPeriode>,
) {
    fun somTidslinje(): Tidslinje<Pair<TimerArbeid, Int>> {
        return Tidslinje(timerArbeidPerPeriode.map {
            Segment(it.periode, it.timerArbeid to it.periode.antallDager())
        }.toList())
    }
}

/**
 * Representerer arbeid i en Periode på et Meldekort.
 */
data class ArbeidIPeriode(val periode: Periode, val timerArbeid: TimerArbeid)
data class FraværForPeriode(
    val periode: Periode,
    val fraværÅrsak: FraværÅrsak,
)

enum class FraværÅrsak {
    SYKDOM_ELLER_SKADE,
    OMSORG_FØRSTE_SKOLEDAG_TILVENNING_ELLER_ANNEN_OPPFØLGING_BARN,
    OMSORG_PLEIE_I_HJEMMET_AV_NÆR_PÅRØRENDE,
    OMSORG_DØDSFALL_I_FAMILIE_ELLER_VENNEKRETS,
    OMSORG_ANNEN_STERK_GRUNN,
    ANNET,
    ;
}
