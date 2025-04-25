package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.adapter.MedlemskapResponse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.komponenter.type.Periode

interface MedlemskapGateway : Gateway {
    fun innhent(person: Person, periode: Periode): List<MedlemskapResponse>
}