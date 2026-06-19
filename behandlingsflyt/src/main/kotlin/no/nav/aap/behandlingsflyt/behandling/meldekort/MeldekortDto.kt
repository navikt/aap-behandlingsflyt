package no.nav.aap.behandlingsflyt.behandling.meldekort

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.helligdagsunntakjustertMeldefrist
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
    val meldefrist: LocalDate,
    val meldekort: MeldekortDto?,
    val tidligereMeldekort: List<MeldekortDto> = emptyList(),
    val meldepliktStatus: Set<MeldepliktStatus>,
)

data class MeldekortDto(
    val journalpostId: String,
    val mottattTidspunkt: LocalDate,
    val oppdatertTidspunkt: LocalDate,
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
)

data class MeldekortProsesseringResponse(
    val meldekortProsesseringStatus: MeldekortProsesseringStatus,
)

fun Meldekort.toDto(
    meldekortData: MeldekortV0?
): MeldekortDto {
    val oppdatertAvSaksbehandler = meldekortData?.opprettetAv != null
    return MeldekortDto(
        journalpostId = journalpostId.identifikator,
        mottattTidspunkt = mottattTidspunkt.toLocalDate(),
        /*
         * Kan ikke bruke mottatt tidspunkt når saksbehandler oppdaterer meldekort da denne datoen som fastsettes
         * manuelt av saksbehandler vil avvike fra når oppdateringen faktisk skjer.
         */
        oppdatertTidspunkt =
            if (oppdatertAvSaksbehandler) opprettetTidspunkt.toLocalDate()
            else mottattTidspunkt.toLocalDate(),
        begrunnelse = meldekortData?.begrunnelse,
        oppdatertAv = meldekortData?.opprettetAv,
        oppdatertAvSaksbehandler = meldekortData?.opprettetAv != null,
        dager = timerArbeidPerPeriode.map { arbeid ->
            DagDto(
                dato = arbeid.periode.fom,
                timerArbeidet = arbeid.timerArbeid.antallTimer.toDouble()
            )
        }.toSet()
    )
}


/**
 * Bygger DTO for én meldeperiode med tilhørende meldekort.
 * Slår opp metadata (begrunnelse / oppdatertAv) fra mottatte dokumenter.
 */
fun toMeldeperiodeMedMeldekortDto(
    meldeperiode: Periode,
    oppfyltMeldeperiodeMedMeldepliktStatus: OppfyltMeldeperiodeMedMeldepliktStatus,
    nyesteMeldekort: Meldekort?,
    tidligereMeldekort: List<Meldekort>,
    mottatteDokumenter: Map<InnsendingReferanse, MottattDokument>,
): MeldeperiodeMedMeldekortDto {
    val meldefrist = helligdagsunntakjustertMeldefrist(meldeperiode.tom.plusDays(8))
    if (nyesteMeldekort == null) {
        return MeldeperiodeMedMeldekortDto(
            meldeperiode = meldeperiode,
            periode = oppfyltMeldeperiodeMedMeldepliktStatus.periode,
            meldefrist = meldefrist,
            meldepliktStatus = oppfyltMeldeperiodeMedMeldepliktStatus.meldepliktStatus,
            meldekort = null,
        )
    }

    return MeldeperiodeMedMeldekortDto(
        meldeperiode = meldeperiode,
        periode = oppfyltMeldeperiodeMedMeldepliktStatus.periode,
        meldefrist = meldefrist,
        meldepliktStatus = oppfyltMeldeperiodeMedMeldepliktStatus.meldepliktStatus,
        meldekort = nyesteMeldekort.toDto(
            meldekortData = mottatteDokumenter.metadataFor(nyesteMeldekort),
        ),
        tidligereMeldekort = tidligereMeldekort
            .sortedByDescending { it.mottattTidspunkt }
            .map { tidligereMeldekort ->
            tidligereMeldekort.toDto(
                meldekortData = mottatteDokumenter.metadataFor(tidligereMeldekort)
            )
        },
    )
}

private fun Map<InnsendingReferanse, MottattDokument>.metadataFor(meldekort: Meldekort): MeldekortV0? =
    this[InnsendingReferanse(meldekort.journalpostId)]?.strukturerteData<MeldekortV0>()?.data