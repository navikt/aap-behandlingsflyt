package no.nav.aap.behandlingsflyt.behandling.behandlerdialog

data class MeldingMedDokumenterDto(
    val melding: MeldingDto,
    val dokumentIdListe: List<DokumentInfoDto>,
)