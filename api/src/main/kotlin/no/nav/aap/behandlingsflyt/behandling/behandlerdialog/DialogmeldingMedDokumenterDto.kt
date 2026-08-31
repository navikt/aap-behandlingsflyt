package no.nav.aap.behandlingsflyt.behandling.behandlerdialog

data class DialogmeldingMedDokumenterDto(
    val dialogmelding: FellesDialogmeldingDto,
    val dokumentIdListe: List<BegrensetDokumentInfoDto>,
)