package no.nav.aap.behandlingsflyt.sakogbehandling.lås

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository
import java.util.*

interface TaSkriveLåsRepository: Repository {
    fun lås(sakId: SakId, behandlingId: BehandlingId): Skrivelås

    fun withLås(sakId: SakId, behandlingId: BehandlingId, block: (it: Skrivelås) -> Unit)

    fun låsBehandling(behandlingId: BehandlingId): BehandlingSkrivelås

    fun withLåstBehandling(behandlingId: BehandlingId, block: (it: BehandlingSkrivelås) -> Unit)

    fun lås(behandlingUUid: UUID): Skrivelås

    fun låsSak(saksnummer: Saksnummer): SakSkrivelås

    fun låsSak(sakId: SakId): SakSkrivelås

    fun verifiserSkrivelås(skrivelås: Skrivelås)

    fun verifiserSkrivelås(skrivelås: SakSkrivelås)

    fun verifiserSkrivelås(skrivelås: BehandlingSkrivelås)
}
