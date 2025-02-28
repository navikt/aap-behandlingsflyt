package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.datadeling.Maksimum
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.gateway.Factory

class FakeApiInternGateway : ApiInternGateway {
    companion object :Factory<ApiInternGateway> {
        override fun konstruer(): ApiInternGateway {
           return FakeApiInternGateway()
        }

    }
    override fun sendPerioder(ident: String, perioder: List<Periode>) {
    }

    override fun sendVedtakData(ident: String, vedtakData: Maksimum) {
    }
}