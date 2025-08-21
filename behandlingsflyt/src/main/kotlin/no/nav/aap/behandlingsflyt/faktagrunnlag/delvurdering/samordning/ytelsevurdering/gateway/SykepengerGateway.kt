package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway

import no.nav.aap.komponenter.gateway.Gateway
import java.time.LocalDate

interface SykepengerGateway : Gateway {
    fun hentYtelseSykepenger(
        personidentifikatorer: Set<String>,
        fom: LocalDate,
        tom: LocalDate
    ): List<UtbetaltePerioder>
}