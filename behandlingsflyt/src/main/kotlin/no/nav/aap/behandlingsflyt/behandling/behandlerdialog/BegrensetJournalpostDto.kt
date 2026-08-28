package no.nav.aap.behandlingsflyt.behandling.behandlerdialog

data class BegrensetJournalpostDto(
    val journalpostId: String?,
    val dokumenter: List<BegrensetDokumentInfoDto>,
    val avsenderMottakerDto: AvsenderMottakerDto?,
)