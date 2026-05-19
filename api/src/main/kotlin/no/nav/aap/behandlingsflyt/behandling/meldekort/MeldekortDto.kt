package no.nav.aap.behandlingsflyt.behandling.meldekort

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.time.LocalDateTime

data class MeldeperioderMedMeldekortResponse(
    val meldeperioderMedMeldekort: Set<MeldeperiodeMedMeldekortDto>,
)

enum class MeldekortProsesseringStatus {
    KLAR,
    PROSESSERER_MELDEKORT,
}

data class MeldeperiodeMedMeldekortDto(
    val meldeperiode: Periode,
    val meldekort: MeldekortDto?,
    val tidligereMeldekort: List<MeldekortDto> = emptyList(),
)

data class MeldekortDto(
    @Deprecated("Bruk journalpostId i stedet for id, da det er mer beskrivende")
    val id: String,
    val journalpostId: String,
    val mottattTidspunkt: LocalDateTime,
    val begrunnelse: String? = null,
    val oppdatertAv: String? = null,
    val dager: Set<DagDto>,
)

data class DagDto(
    val dato: LocalDate,
    val timerArbeidet: Double
)

data class OppdaterMeldekortRequest(
    val meldeperiode: Periode,
    val begrunnelse: String,
    val dager: Set<DagDto>,
)

data class OppdaterMeldekortResponse(
    val journalpostId: String,
)

data class MeldekortProsesseringResponse(
    val meldekortProsesseringStatus: MeldekortProsesseringStatus,
)

fun Meldekort.toDto(begrunnelse: String?, oppdatertAv: String?): MeldekortDto = MeldekortDto(
    id = journalpostId.identifikator,
    journalpostId = journalpostId.identifikator,
    mottattTidspunkt = mottattTidspunkt,
    begrunnelse = begrunnelse,
    oppdatertAv = oppdatertAv,
    dager = timerArbeidPerPeriode.map { arbeid ->
        DagDto(
            dato = arbeid.periode.fom,
            timerArbeidet = arbeid.timerArbeid.antallTimer.toDouble()
        )
    }.toSet()
)