package no.nav.aap.behandlingsflyt.behandling.meldekort

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.tilgang.plugin.kontrakt.Saksreferanse
import java.time.Instant
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

data class OppdaterMeldekortRequest(
    val saksnummer: String,
    val meldeperiode: Periode,
    val begrunnelse: String,
    val dager: Set<DagDto>,
) : Saksreferanse {
    override fun hentSaksreferanse(): String = saksnummer
}

data class OppdaterMeldekortResponse(
    val journalpostId: String,
)

fun Meldekort.toDto(): MeldekortDto = MeldekortDto(
    id = journalpostId.identifikator,
    mottattTidspunkt = mottattTidspunkt,
    dager = timerArbeidPerPeriode.map { arbeid ->
        DagDto(
            dato = arbeid.periode.fom,
            timerArbeidet = arbeid.timerArbeid.antallTimer.toDouble()
        )
    }.toSet()
)