package no.nav.aap.behandlingsflyt.hendelse.datadeling

import no.nav.aap.behandlingsflyt.datadeling.Maksimum
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.gateway.Gateway

interface ApiInternGateway : Gateway {
    fun sendPerioder(ident: String, perioder: List<Periode>)
    fun sendVedtakData(ident: String, vedtakData: Maksimum)
}

