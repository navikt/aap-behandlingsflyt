package no.nav.aap.behandlingsflyt.behandling.meldekort

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.time.LocalDateTime

data class MeldeperioderMedMeldekortResponse(
    val meldeperioderMedMeldekort: Set<MeldeperiodeMedMeldekortDto>,
    val meldekortProsesseringStatus: MeldekortProsesseringStatus = MeldekortProsesseringStatus.KLAR,
)

enum class MeldekortProsesseringStatus {
    KLAR,
    PROSESSERER_MELDEKORT,
}

data class MeldeperiodeMedMeldekortDto(
    val meldeperiode: Periode,
    val meldekort: MeldekortDto?
)

data class MeldekortDto(
    val id: String,
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

fun Meldekort.toDto(begrunnelse: String?, oppdatertAv: String?): MeldekortDto = MeldekortDto(
    id = journalpostId.identifikator,
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