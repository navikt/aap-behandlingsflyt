package no.nav.aap.behandlingsflyt.behandling.meldekort

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.time.LocalDateTime

data class MeldeperioderMedMeldekortResponse(
    val meldeperioderMedMeldekort: Set<MeldeperiodeMedMeldekortDto>,
)

data class MeldeperiodeMedMeldekortDto(
    val meldeperiode: Periode,
    val meldekort: MeldekortDto?
)

data class MeldekortDto(
    val id: String,
    val mottattTidspunkt: LocalDateTime,
    val dager: Set<DagDto>,
)

data class DagDto(
    val dato: LocalDate,
    val timerArbeidet: Double
)

fun Meldekort.toDto(): MeldekortDto = MeldekortDto(
    id = referanse.verdi,
    mottattTidspunkt = mottattTidspunkt,
    dager = timerArbeidPerPeriode.map { arbeid ->
        DagDto(
            dato = arbeid.periode.fom,
            timerArbeidet = arbeid.timerArbeid.antallTimer.toDouble()
        )
    }.toSet()
)