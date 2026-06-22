package no.nav.aap.behandlingsflyt.behandling.meldekort

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.komponenter.type.Periode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

data class MeldekortPdfRequest(
    val ident: String,
    val sendtInnDato: String,
    val meldeDato: String,
    val utførtAv: String,
    val begrunnelse: String?,
    val sammenlagtArbeidIPerioden: Int,
    val meldeperiode: MeldekortMeldeperiode,
    val meldekort: MeldekortPdfData
)

data class MeldekortMeldeperiode(
    val fraOgMedDato: String,
    val tilOgMedDato: String,
    val uker: String
)

data class MeldekortPdfData(
    val timerArbeidPerUkeIPerioden: List<MeldekortUke>
)

data class MeldekortUke(
    val ukenummer: String,
    val fraOgMedDato: String,
    val tilOgMedDato: String,
    val dager: List<MeldekortDag>
)

data class MeldekortDag(
    val dag: String,
    val timerArbeid: String
)

fun MeldekortV0.tilPdfRequest(
    ident: String,
    meldeperiode: Periode,
    utførtAv: String,
    tidspunkt: Instant,
    meldeDato: LocalDate,
): MeldekortPdfRequest {
    val ukeFields = WeekFields.of(Locale.of("nb", "NO"))
    val datoFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val norsk = Locale.of("nb", "NO")

    val uker = timerArbeidPerPeriode
        .filter { it.timerArbeid > 0}
        .groupBy { it.fraOgMedDato.get(ukeFields.weekOfWeekBasedYear()) }
        .map { (ukenummer, dager) ->
            MeldekortUke(
                ukenummer = ukenummer.toString(),
                fraOgMedDato = dager.minOf { it.fraOgMedDato }.format(datoFormatter),
                tilOgMedDato = dager.maxOf { it.tilOgMedDato }.format(datoFormatter),
                dager = dager.map { dag ->
                    MeldekortDag(
                        dag = dag.fraOgMedDato.dayOfWeek.getDisplayName(TextStyle.FULL, norsk),
                        timerArbeid = formaterTimer(dag.timerArbeid)
                    )
                }
            )
        }

    val uke1 = meldeperiode.fom.get(ukeFields.weekOfWeekBasedYear())
    val uke2 = meldeperiode.tom.get(ukeFields.weekOfWeekBasedYear())

    return MeldekortPdfRequest(
        ident = ident,
        sendtInnDato = tidspunkt.atZone(ZoneId.of("Europe/Oslo")).toLocalDate().format(datoFormatter),
        meldeDato = meldeDato.format(datoFormatter),
        utførtAv = utførtAv,
        sammenlagtArbeidIPerioden = timerArbeidPerPeriode.sumOf { it.timerArbeid }.toInt(),
        meldeperiode = MeldekortMeldeperiode(
            fraOgMedDato = meldeperiode.fom.format(datoFormatter),
            tilOgMedDato = meldeperiode.tom.format(datoFormatter),
            uker = "uke $uke1 - $uke2"
        ),
        begrunnelse = this.begrunnelse,
        meldekort = MeldekortPdfData(timerArbeidPerUkeIPerioden = uker)
    )
}

fun formaterTimer(number: Double?): String {
    return when {
        number == null -> ""
        number % 1.0 == 0.0 -> number.toInt().toString()
        else -> number.toString()
    }
}