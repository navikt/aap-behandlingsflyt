package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway

import no.nav.aap.komponenter.gateway.Gateway
import java.time.LocalDate

interface DagpengerGateway : Gateway {
    fun hentYtelseDagpenger(
        personidentifikatorer: String,
        fom: LocalDate,
        tom: LocalDate
    ): List<DagpengerPeriode>
}