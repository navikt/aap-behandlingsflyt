package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.KandidatForPåminnelseRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import java.time.LocalDate
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status as AvklaringsbehovStatus
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status as BehandlingStatus

object InMemoryKandidatForPåminnelseRepository : KandidatForPåminnelseRepository {
    override fun finnKandidaterForPåminnelse(bestillingOpprettetDato: LocalDate): List<BehandlingReferanse> =
        InMemoryBehandlingRepository.alle()
            .filter { it.status() in setOf(BehandlingStatus.UTREDES, BehandlingStatus.OPPRETTET) }
            .filter { behandling ->
                InMemoryAvklaringsbehovRepository.hent(behandling.id).any { behov ->
                    behov.definisjon == Definisjon.BESTILL_LEGEERKLÆRING && behov.historikk.any {
                        it.status == AvklaringsbehovStatus.OPPRETTET &&
                            it.tidsstempel.toLocalDate() == bestillingOpprettetDato
                    }
                }
            }
            .filter { behandling ->
                behandling.vurderingsbehov().none {
                    it.type in setOf(Vurderingsbehov.MOTTATT_LEGEERKLÆRING, Vurderingsbehov.MOTTATT_DIALOGMELDING) &&
                        !it.oppdatertTid.toLocalDate().isBefore(bestillingOpprettetDato)
                }
            }
            .map { it.referanse }
}
