package no.nav.aap.behandlingsflyt.sakogbehandling.lås

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.repository.Repository
import java.util.*

interface TaSkriveLåsRepository: Repository {
    fun lås(sakId: SakId, behandlingId: BehandlingId): Skrivelås

    fun låsBehandling(behandlingId: BehandlingId): BehandlingSkrivelås

    fun lås(behandlingUUid: UUID): Skrivelås

    fun låsSak(saksnummer: Saksnummer): SakSkrivelås

    fun låsSak(sakId: SakId): SakSkrivelås

    fun verifiserSkrivelås(skrivelås: Skrivelås)

    fun verifiserSkrivelås(skrivelås: SakSkrivelås)

    fun verifiserSkrivelås(skrivelås: BehandlingSkrivelås)
}
