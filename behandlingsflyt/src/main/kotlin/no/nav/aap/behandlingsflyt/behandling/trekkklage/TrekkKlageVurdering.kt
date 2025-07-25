package no.nav.aap.behandlingsflyt.behandling.trekkklage

import no.nav.aap.behandlingsflyt.behandling.trekkklage.flate.TrekkKlageVurderingDto
import no.nav.aap.behandlingsflyt.behandling.trekkklage.flate.TrekkKlageÅrsakDto
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant

data class TrekkKlageVurdering(
    val begrunnelse: String,
    val skalTrekkes: Boolean,
    val hvorforTrekkes: TrekkKlageÅrsak?,
    val vurdertAv: Bruker,
    val vurdert: Instant,
)

enum class TrekkKlageÅrsak {
    TRUKKET_AV_BRUKER,
    FEILREGISTRERING;
}

fun TrekkKlageVurdering.tilDto(): TrekkKlageVurderingDto =
    TrekkKlageVurderingDto(
        begrunnelse = this.begrunnelse,
        skalTrekkes = this.skalTrekkes,
        hvorforTrekkes = when(this.hvorforTrekkes) {
            TrekkKlageÅrsak.FEILREGISTRERING -> TrekkKlageÅrsakDto.FEILREGISTRERING
            TrekkKlageÅrsak.TRUKKET_AV_BRUKER -> TrekkKlageÅrsakDto.TRUKKET_AV_BRUKER
            null -> null
        }
    )