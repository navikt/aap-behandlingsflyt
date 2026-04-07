package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.behandling.ansattinfo.Enhet
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.EnhetGateway
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.EnhetsType
import no.nav.aap.komponenter.gateway.Factory

class FakeEnhetGateway: EnhetGateway {
    override fun hentEnhet(enhetsnummer: String): Enhet {
        return Enhet(
            enhetsNummer = enhetsnummer,
            navn = "Enhet1",
            type = EnhetsType.ARBEID_OG_YTELSE,
        )
    }

    override fun hentAlleEnheter(): List<Enhet> {
        return listOf(hentEnhet("0300"))
    }

    companion object : Factory<EnhetGateway> {
        override fun konstruer(): EnhetGateway = FakeEnhetGateway()
    }

}