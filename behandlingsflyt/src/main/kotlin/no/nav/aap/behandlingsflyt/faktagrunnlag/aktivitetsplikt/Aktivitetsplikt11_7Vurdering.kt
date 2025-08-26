package no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt

import java.time.Instant
import java.time.LocalDate

data class Aktivitetsplikt11_7Vurdering(
    val begrunnelse: String,
    val erOppfylt: Boolean,
    val utfall: Utfall? = null, 
    val vurdertAv: String,
    val gjelderFra: LocalDate,
    val opprettet: Instant
)

enum class Utfall {
    STANS, OPPHØR
}
