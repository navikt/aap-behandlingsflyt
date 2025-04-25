package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.Personinfo
import no.nav.aap.komponenter.gateway.Gateway

interface PersoninfoBulkGateway : Gateway {
    fun hentPersoninfoForIdenter(identer: List<Ident>): List<Personinfo>
}