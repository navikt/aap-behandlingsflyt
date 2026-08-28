package no.nav.aap.behandlingsflyt.behandling.behandlerdialog

data class AvsenderMottakerDto(
    val id: String?,
    val type: AvsenderMottakerIdType?,
    val navn: String?,
) {
    enum class AvsenderMottakerIdType {
        FNR,
        ORGNR,
        HPRNR,
        UTL_ORG,
        NULL,
        UKJENT,
    }
}