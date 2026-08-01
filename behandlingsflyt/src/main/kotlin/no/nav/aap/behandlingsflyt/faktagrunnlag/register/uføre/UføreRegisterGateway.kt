package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.beregning.Uføre
import no.nav.aap.komponenter.gateway.Gateway
import java.time.LocalDate
import no.nav.aap.misc.uføre.UføreSøknad

interface UføreRegisterGateway : Gateway {
    fun innhentMedHistorikk(
        person: Person,
        fraDato: LocalDate
    ): Set<Uføre>

    fun hentÅpenUføreSøknad(
        person: Person,
    ): UføreSøknad?
}