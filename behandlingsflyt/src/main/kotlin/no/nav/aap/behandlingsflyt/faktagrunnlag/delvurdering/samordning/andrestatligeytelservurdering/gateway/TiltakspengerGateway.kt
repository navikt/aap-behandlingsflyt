package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway

import no.nav.aap.komponenter.gateway.Gateway
import java.time.LocalDate

interface TiltakspengerGateway : Gateway {
    fun hentYtelseTiltakspenger(
        personidentifikatorer: String,
        fom: LocalDate,
        tom: LocalDate
    ): List<TiltakspengerPeriode>
}