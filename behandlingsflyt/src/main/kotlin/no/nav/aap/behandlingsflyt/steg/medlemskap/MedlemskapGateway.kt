package no.nav.aap.behandlingsflyt.steg.medlemskap

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.medlemskap.MedlemskapDataIntern

interface MedlemskapGateway : Gateway {
    fun innhent(person: Person, periode: Periode): List<MedlemskapDataIntern>
}