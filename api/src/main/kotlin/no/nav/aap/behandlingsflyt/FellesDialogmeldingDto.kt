package no.nav.aap.behandlingsflyt

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

data class DialogmeldingMedDokumenter(
    val dialogmelding: FellesDialogmeldingDto,
    val dokumentIdListe: MutableList<BegrensetDokumentInfoDto>
)

data class BegrensetDokumentInfoDto(
    val dokumentInfoId: String,
    val tittel: String?
)