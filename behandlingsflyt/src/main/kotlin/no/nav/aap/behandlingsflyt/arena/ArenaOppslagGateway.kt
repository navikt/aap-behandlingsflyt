package no.nav.aap.behandlingsflyt.arena

import no.nav.aap.komponenter.gateway.Gateway

interface ArenaOppslagGateway : Gateway {
    fun hentHarHistorikk(personidentifikator: String): HarHistorikkResponse
    fun hentSakerForPerson(personidentifikator: String): SakerResponse
}
