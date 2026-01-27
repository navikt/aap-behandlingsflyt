package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway

import no.nav.aap.komponenter.gateway.Gateway

interface DagpengerGateway : Gateway {
    fun hentYtelseDagpenger(
        personidentifikatorer: Set<String>,
        fom: String,
        tom: String
    ): List<DagpengerPeriode>
}