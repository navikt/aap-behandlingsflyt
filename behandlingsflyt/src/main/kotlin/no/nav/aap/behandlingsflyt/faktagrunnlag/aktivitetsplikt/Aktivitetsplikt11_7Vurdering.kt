package no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt

import java.time.Instant

data class Aktivitetsplikt11_7Vurdering(
    val begrunnelse: String,
    val erOppfylt: Boolean,
    val utfall: Utfall? = null, 
    val vurdertAv: String,
    val opprettet: Instant
)

enum class Utfall {
    STANS, OPPHØR
}
