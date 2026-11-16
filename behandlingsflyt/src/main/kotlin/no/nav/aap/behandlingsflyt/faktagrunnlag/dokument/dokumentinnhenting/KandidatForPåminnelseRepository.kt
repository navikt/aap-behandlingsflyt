package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.komponenter.repository.Repository
import java.time.LocalDate

interface KandidatForPåminnelseRepository : Repository {
    fun finnKandidaterForPåminnelse(bestillingOpprettetDato: LocalDate): List<BehandlingReferanse>
}
