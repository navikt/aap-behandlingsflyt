package no.nav.aap.behandlingsflyt.behandling.ansattinfo

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.navenheter.Enhet
import no.nav.aap.komponenter.gateway.Gateway

interface EnhetGateway : Gateway {
    fun hentEnhet(enhetsnummer: String): Enhet
    fun hentAlleEnheter(): List<Enhet>
}