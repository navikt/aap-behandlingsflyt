package no.nav.aap.behandlingsflyt.help

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.BehandlingSkrivelås
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.SakSkrivelås
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.Skrivelås
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import java.util.*

object StubTaSkrivelåsRepository : TaSkriveLåsRepository {
    override fun lås(sakId: SakId, behandlingId: BehandlingId) = Skrivelås(
        sakSkrivelås = låsSak(SakId(0L)),
        behandlingSkrivelås = låsBehandling(BehandlingId(0L)),
    )

    override fun withLås(
        sakId: SakId,
        behandlingId: BehandlingId,
        block: (it: Skrivelås) -> Unit
    ) {
    }

    override fun låsBehandling(behandlingId: BehandlingId) = BehandlingSkrivelås(
        id = behandlingId,
        versjon = 0L,
    )

    override fun withLåstBehandling(
        behandlingId: BehandlingId,
        block: (it: BehandlingSkrivelås) -> Unit
    ) {
    }

    override fun lås(behandlingUUid: UUID) = Skrivelås(
        sakSkrivelås = låsSak(SakId(0L)),
        behandlingSkrivelås = låsBehandling(BehandlingId(0L)),
    )

    override fun låsSak(sakId: SakId) = SakSkrivelås(
        id = sakId,
        versjon = 0L,
        taSkriveLåsRepository = this,
    )

    override fun verifiserSkrivelås(skrivelås: Skrivelås) {}

    override fun verifiserSkrivelås(skrivelås: SakSkrivelås) {}

    override fun verifiserSkrivelås(skrivelås: BehandlingSkrivelås) {}

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
    }

    override fun slett(behandlingId: BehandlingId) {}
}