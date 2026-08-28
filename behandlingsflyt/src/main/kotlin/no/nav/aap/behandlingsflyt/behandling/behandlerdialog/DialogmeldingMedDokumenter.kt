package no.nav.aap.behandlingsflyt.behandling.behandlerdialog

data class DialogmeldingMedDokumenter(
    val dialogmelding: FellesDialogmeldingDto,
    val dokumentIdListe: List<BegrensetDokumentInfoDto>,
)