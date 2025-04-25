package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.gateway.Gateway
import java.time.LocalDate

interface UføreRegisterGateway : Gateway {
    @Deprecated("Henter kun for en gitt dag - bruk heller innhentMedHistorikk som henter for hele perioden fra en gitt dato")
    fun innhent(
        person: Person,
        forDato: LocalDate
    ): List<Uføre>

    fun innhentMedHistorikk(
        person: Person,
        fraDato: LocalDate
    ): List<Uføre>
}