package no.nav.aap.behandlingsflyt.kontrakt.hendelse

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakResultat
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakV0
import java.time.LocalDate

public data class UførevedtakDto (
    val resultat: UførevedtakResultatDto,
    val virkningsdato: LocalDate
)

public enum class UførevedtakResultatDto {
    OPPHØR,
    INNVILGELSE,
    AVSLAG,
    ENDRET
}

public fun UførevedtakV0.tilUføreVedtakDto(): UførevedtakDto {
    return UførevedtakDto(
        resultat = this.resultat.tilDto(),
        virkningsdato = this.virkningsdato,
    )
}

private fun UførevedtakResultat.tilDto(): UførevedtakResultatDto {
    return when (this) {
        UførevedtakResultat.OPPH -> UførevedtakResultatDto.OPPHØR
        UførevedtakResultat.INNV -> UførevedtakResultatDto.INNVILGELSE
        UførevedtakResultat.AVSL -> UførevedtakResultatDto.AVSLAG
        UførevedtakResultat.ENDR -> UførevedtakResultatDto.ENDRET

    }
}