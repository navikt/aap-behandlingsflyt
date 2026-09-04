package no.nav.aap.behandlingsflyt.behandling.behandlerdialog

data class MeldingMedDokumenterDto(
    val dialogmelding: MeldingDto,
    val dokumentIdListe: List<DokumentInfoDto>,
)