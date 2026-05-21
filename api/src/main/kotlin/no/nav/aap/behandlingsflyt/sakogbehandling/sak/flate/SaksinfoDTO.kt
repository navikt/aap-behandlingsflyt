package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ResultatKode
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDateTime

data class SaksinfoDTO(
    val saksnummer: String,
    val opprettetTidspunkt: LocalDateTime,
    val periode: Periode,
    val ident: String,
    val tilstand: SakTilstandDto? = null,
    @Deprecated("Bruk tilstand i stedet for å utlede sakens tilstand")
    val resultat: ResultatKode? = null
)

enum class SakTilstandDto {
    ÅPEN,
    TRUKKET,
    ;

    companion object {
        fun fra(resultat: Resultat?) = when (resultat) {
            Resultat.TRUKKET -> TRUKKET
            else -> ÅPEN
        }
    }
}
