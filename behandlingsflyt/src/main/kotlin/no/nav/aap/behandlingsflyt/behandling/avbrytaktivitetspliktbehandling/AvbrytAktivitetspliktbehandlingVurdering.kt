package no.nav.aap.behandlingsflyt.behandling.avbrytaktivitetspliktbehandling

import no.nav.aap.komponenter.verdityper.Bruker

data class AvbrytAktivitetspliktbehandlingVurdering(
    val årsak: AvbrytAktivitetspliktbehandlingÅrsak,
    val begrunnelse: String,
    val vurdertAv: Bruker
)

enum class AvbrytAktivitetspliktbehandlingÅrsak {
    BEHANDLINGEN_BLE_OPPRETTET_VED_EN_FEIL,
    DET_HAR_OPPSTAATT_EN_FEIL_OG_BEHANDLINGEN_MAA_STARTES_PAA_NYTT
}