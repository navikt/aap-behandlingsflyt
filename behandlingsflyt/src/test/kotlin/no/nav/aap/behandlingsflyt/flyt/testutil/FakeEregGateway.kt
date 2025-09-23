package no.nav.aap.behandlingsflyt.flyt.testutil

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.ereg.EnhetsregisteretGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.ereg.Organisasjonsnummer
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.ereg.adapter.EnhetsregisterOrganisasjonResponse
import no.nav.aap.komponenter.gateway.Factory

class FakeEregGateway: EnhetsregisteretGateway {
    override fun hentEREGData(organisasjonsnummer: Organisasjonsnummer): EnhetsregisterOrganisasjonResponse? {
        return null
    }

    companion object : Factory<EnhetsregisteretGateway> {
        override fun konstruer(): EnhetsregisteretGateway = FakeEregGateway()
    }
}