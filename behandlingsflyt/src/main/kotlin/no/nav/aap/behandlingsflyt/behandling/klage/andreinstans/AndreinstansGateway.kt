package no.nav.aap.behandlingsflyt.behandling.klage.andreinstans

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.KlageResultat
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.gateway.Gateway
import java.time.LocalDate

interface AndreinstansGateway: Gateway {
    fun oversendTilAndreinstans(
        saksnummer: Saksnummer, // TODO: Håndter Arena
        behandlingsreferanse: BehandlingReferanse,
        kravDato: LocalDate,
        klagenGjelder: Person,
        klageresultat: KlageResultat,
        saksbehandlersEnhet: String,
    )
}