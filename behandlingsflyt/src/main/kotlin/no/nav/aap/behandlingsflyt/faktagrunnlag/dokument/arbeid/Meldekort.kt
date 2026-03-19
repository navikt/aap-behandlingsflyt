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
    val fravær: Set<FraværIPeriode>,
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
data class ArbeidIPeriode(
    val periode: Periode,
    val timerArbeid: TimerArbeid
)

// Hvordan representere meldekort fravær i en periodisert tidslinje i behandlingsflyt?
// ArbeidIPeriode har kun 1 typer verdi, antall timer, og kan representere dette i samme tidslinje med ulike
// perioder da verdien kan summeres. Men mister vi da detaljnivå timer per dag ?
// Men FraværIPeriode representerer en rekke ulike typer verdier, FraværÅrsak, og
// disse kan ikke slås sammen i samme tidslinje. Og de må beholde detaljnivå på dag for å kunne "avregnes" mot
// arbeidstimer samme dag etc.
// Har vi noen eksempler på periodiserte verdier som minner om dette i behandlingsflyt og hvordan skal vi representere
// Fravær periodisert i tidslinje for å ivareta beregninger og resultater sammen med andre ytelser/graderinger/osv. ?
data class FraværIPeriode(
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
