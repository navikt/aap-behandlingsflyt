package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.forretningsflyt.gjenopptak.GjenopptakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.SakOgBehandling
import java.time.LocalDate
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status as AvklaringsbehovStatus
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status as BehandlingStatus

object InMemoryGjenopptakRepository : GjenopptakRepository {
    override fun finnBehandlingerForGjennopptak(): List<SakOgBehandling> {
        val iDag = LocalDate.now()
        return InMemoryBehandlingRepository.alle()
            .filter { it.status() in setOf(BehandlingStatus.UTREDES, BehandlingStatus.OPPRETTET, BehandlingStatus.IVERKSETTES) }
            .filter { behandling ->
                InMemoryAvklaringsbehovRepository.hent(behandling.id).any { behov ->
                    val sisteEndring = behov.historikk.maxOrNull()
                    sisteEndring?.status == AvklaringsbehovStatus.OPPRETTET &&
                        sisteEndring.frist?.let { !it.isAfter(iDag) } == true
                }
            }
            .map { SakOgBehandling(it.sakId, it.id) }
    }
}
