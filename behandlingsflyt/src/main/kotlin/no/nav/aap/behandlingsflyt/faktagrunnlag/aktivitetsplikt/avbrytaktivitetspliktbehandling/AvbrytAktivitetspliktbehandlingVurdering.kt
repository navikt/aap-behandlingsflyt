package no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.avbrytaktivitetspliktbehandling

import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDateTime

data class AvbrytAktivitetspliktbehandlingVurdering(
    val årsak: AvbrytAktivitetspliktbehandlingÅrsak,
    val begrunnelse: String,
    val vurdertAv: Bruker,
    val opprettetTidspunkt: LocalDateTime? = null,
)

enum class AvbrytAktivitetspliktbehandlingÅrsak {
    BEHANDLINGEN_BLE_OPPRETTET_VED_EN_FEIL,
    DET_HAR_OPPSTAATT_EN_FEIL_OG_BEHANDLINGEN_MAA_STARTES_PAA_NYTT
}