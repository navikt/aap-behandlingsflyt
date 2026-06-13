package no.nav.aap.behandlingsflyt.behandling.meldekort

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ArbeidIPeriodeV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
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
    val meldepliktStatus: Set<MeldepliktStatus>,
)

data class MeldekortDto(
    @Deprecated("Bruk journalpostId i stedet for id, da det er mer beskrivende")
    val id: String,
    val journalpostId: String,
    @Deprecated("Bruk heller meldeDato fra MeldeperiodeMedMeldekortDto")
    val meldeDato: LocalDate,
    val mottattTidspunkt: LocalDate? = null,
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
) {
    fun tilMeldekort(vurdertAv: Bruker): MeldekortV0 =
        MeldekortV0(
            harDuArbeidet = dager
                .takeIf { it.isNotEmpty() }?.let { it.sumOf { dag -> dag.timerArbeidet } > 0.0 },
            opprettetAv = vurdertAv.ident,
            begrunnelse = begrunnelse,
            timerArbeidPerPeriode = dager.map {
                ArbeidIPeriodeV0(
                    fraOgMedDato = it.dato,
                    tilOgMedDato = it.dato,
                    timerArbeid = it.timerArbeidet,
                )
            }
        )
}

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
    oppdatertAvSaksbehandler: Boolean
): MeldekortDto =
    MeldekortDto(
        id = journalpostId.identifikator,
        journalpostId = journalpostId.identifikator,
        meldeDato = meldeDato ?: mottattTidspunkt.toLocalDate(),
        mottattTidspunkt = mottattTidspunkt.toLocalDate(),
        oppdatertTidspunkt = opprettetTidspunkt.toLocalDate(),
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