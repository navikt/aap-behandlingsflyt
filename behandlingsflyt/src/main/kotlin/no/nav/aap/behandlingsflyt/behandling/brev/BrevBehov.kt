package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev

class BrevBehov(val typeBrev: TypeBrev?) {
    fun harBehovForBrev(): Boolean {
        return typeBrev != null
    }
}