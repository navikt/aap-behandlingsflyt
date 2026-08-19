package no.nav.aap.behandlingsflyt.kontrakt.hendelse

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakResultat
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakV0
import java.time.LocalDate

data class UførevedtakDto (
    val resultat: UførevedtakResultatDto,
    val virkningsdato: LocalDate
)

enum class UførevedtakResultatDto {
    OPPHØR,
    INNVILGELSE,
    AVSLAG,
    ENDRET
}

fun UførevedtakV0.tilUføreVedtakDto(): UførevedtakDto {
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