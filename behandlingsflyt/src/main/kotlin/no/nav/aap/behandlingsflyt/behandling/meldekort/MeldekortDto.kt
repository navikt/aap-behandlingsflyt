package no.nav.aap.behandlingsflyt.behandling.meldekort

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
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
    val tidligereMeldekort: List<MeldekortDto> = emptyList(),
    val meldepliktStatus: Set<MeldepliktStatus>,
)

data class MeldekortDto(
    val journalpostId: String,
    val mottattTidspunkt: LocalDate? = null,
    val oppdatertTidspunkt: LocalDate? = null,
    val oppdatertAv: String? = null,
    val oppdatertAvSaksbehandler: Boolean,
    val begrunnelse: String? = null,
    val dager: Set<DagDto>,
)

data class DagDto(
    val dato: LocalDate,
    val timerArbeidet: Double
)

data class OppdaterMeldekortResponse(
    val journalpostId: String,
    val oppdatertTidspunkt: LocalDate,
)

data class MeldekortProsesseringResponse(
    val meldekortProsesseringStatus: MeldekortProsesseringStatus,
)

fun Meldekort.toDto(
    begrunnelse: String?,
    oppdatertAv: String?,
    oppdatertAvSaksbehandler: Boolean
): MeldekortDto =
    MeldekortDto(
        journalpostId = journalpostId.identifikator,
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

/**
 * Bygger DTO for én meldeperiode med tilhørende meldekort.
 * Slår opp metadata (begrunnelse / oppdatertAv) fra mottatte dokumenter.
 */
fun toMeldeperiodeMedMeldekortDto(
    meldeperiode: Periode,
    meldeperiodeData: OppfyltMeldeperiodeMedMeldepliktStatus,
    gjeldendeMeldekort: Meldekort?,
    tidligereMeldekort: List<Meldekort>,
    mottatteDokumenter: Map<InnsendingReferanse, MottattDokument>,
): MeldeperiodeMedMeldekortDto {
    if (gjeldendeMeldekort == null) {
        return MeldeperiodeMedMeldekortDto(
            meldeperiode = meldeperiode,
            periode = meldeperiodeData.periode,
            meldepliktStatus = meldeperiodeData.meldepliktStatus,
            meldekort = null,
        )
    }

    val meldekortData = mottatteDokumenter.metadataFor(gjeldendeMeldekort)

    // Fallback til bruker dersom opprettetAv er null – settes eksplisitt ved korrigering
    val oppdatertAvSaksbehandler = meldekortData?.opprettetAv != null

    return MeldeperiodeMedMeldekortDto(
        meldeperiode = meldeperiode,
        periode = meldeperiodeData.periode,
        meldepliktStatus = meldeperiodeData.meldepliktStatus,
        meldekort = gjeldendeMeldekort.toDto(
            begrunnelse = meldekortData?.begrunnelse,
            oppdatertAv = meldekortData?.opprettetAv,
            oppdatertAvSaksbehandler = oppdatertAvSaksbehandler,
        ),
        tidligereMeldekort = tidligereMeldekort.map { tidligere ->
            val data = mottatteDokumenter.metadataFor(tidligere)
            tidligere.toDto(
                begrunnelse = data?.begrunnelse,
                oppdatertAv = data?.opprettetAv,
                oppdatertAvSaksbehandler = oppdatertAvSaksbehandler,
            )
        },
    )
}

private fun Map<InnsendingReferanse, MottattDokument>.metadataFor(meldekort: Meldekort): MeldekortV0? =
    this[InnsendingReferanse(meldekort.journalpostId)]?.strukturerteData<MeldekortV0>()?.data