package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.gateway.Gateway
import java.time.LocalDate

interface UføreRegisterGateway : Gateway {
    fun innhentMedHistorikk(
        person: Person,
        fraDato: LocalDate
    ): Set<Uføre>
}