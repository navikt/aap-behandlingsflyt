package no.nav.aap.behandlingsflyt.behandling.meldekort

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class MeldeperioderMedMeldekortResponse(
    val meldeperioderMedMeldekort: Set<MeldeperiodeMedMeldekortDto>,
)

enum class MeldekortProsesseringStatus {
    KLAR,
    PROSESSERER_MELDEKORT,
}

data class MeldeperiodeMedMeldekortDto(
    val meldeperiode: Periode,
    val periode: Periode?,
    val meldekort: MeldekortDto?,
    val meldeDato: LocalDate?,
    val tidligereMeldekort: List<MeldekortDto> = emptyList(),
)

data class MeldekortDto(
    @Deprecated("Bruk journalpostId i stedet for id, da det er mer beskrivende")
    val id: String,
    val journalpostId: String,
    @Deprecated("Bruk heller meldeDato fra MeldeperiodeMedMeldekortDto")
    val meldeDato: LocalDate,
    val oppdatertTidspunkt: LocalDate? = null,
    val begrunnelse: String? = null,
    val oppdatertAv: String? = null,
    val oppdatertAvSaksbehandler: Boolean,
    val dager: Set<DagDto>,
)

data class DagDto(
    val dato: LocalDate,
    val timerArbeidet: Double
)

data class OppdaterMeldekortRequest(
    val meldeperiode: Periode,
    val meldeDato: LocalDate,
    val begrunnelse: String,
    val dager: Set<DagDto>,
)

data class OppdaterMeldekortResponse(
    val journalpostId: String,
    val oppdatertTidspunkt: LocalDate,
)

data class MeldekortProsesseringResponse(
    val meldekortProsesseringStatus: MeldekortProsesseringStatus,
)

fun Meldekort.toDto(
    meldeDato: LocalDate?,
    begrunnelse: String?,
    oppdatertAv: String?,
    oppdatertTidspunkt: LocalDate?,
    oppdatertAvSaksbehandler: Boolean
): MeldekortDto =
    MeldekortDto(
        id = journalpostId.identifikator,
        journalpostId = journalpostId.identifikator,
        meldeDato = meldeDato ?: mottattTidspunkt.toLocalDate(),
        oppdatertTidspunkt = oppdatertTidspunkt,
        begrunnelse = begrunnelse,
        oppdatertAv = oppdatertAv,
        oppdatertAvSaksbehandler = oppdatertAvSaksbehandler,
        dager = timerArbeidPerPeriode.map { arbeid ->
            DagDto(
                dato = arbeid.periode.fom,
                timerArbeidet = arbeid.timerArbeid.antallTimer.toDouble()
            )
        }.toSet()
    )