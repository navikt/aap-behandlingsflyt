package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.gateway.Gateway
import java.time.Year
import no.nav.aap.misc.inntekt.InntektPerÅrFraRegister

interface InntektRegisterGateway : Gateway {
    fun innhent(
        person: Person,
        år: Set<Year>
    ): Set<InntektPerÅrFraRegister>
}

