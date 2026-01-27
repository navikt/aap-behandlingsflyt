package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway

import no.nav.aap.komponenter.gateway.Gateway

interface DagpengerGateway : Gateway {
    fun hentYtelseDagpenger(
        personidentifikatorer: String,
        fom: String,
        tom: String
    ): List<DagpengerPeriode>
}