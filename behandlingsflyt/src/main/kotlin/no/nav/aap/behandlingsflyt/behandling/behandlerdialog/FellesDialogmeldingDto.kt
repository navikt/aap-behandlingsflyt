package no.nav.aap.behandlingsflyt.behandling.behandlerdialog

import no.nav.aap.behandlingsflyt.behandling.dialogmelding.DialogmeldingLeveringStatus
import no.nav.aap.behandlingsflyt.behandling.dialogmelding.InnkommendeUtgaaende
import no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType
import java.time.LocalDateTime

data class FellesDialogmeldingDto(
    val innkommendeUtgaaende: InnkommendeUtgaaende,
    val meldingFraNavn: String,
    val opprettetTidspunkt: LocalDateTime,
    val dokumentasjonsType: DokumentasjonType?,
    val tekst: String?,
    val meldingStatus: DialogmeldingLeveringStatus?,
    val journalpostId: String?
)