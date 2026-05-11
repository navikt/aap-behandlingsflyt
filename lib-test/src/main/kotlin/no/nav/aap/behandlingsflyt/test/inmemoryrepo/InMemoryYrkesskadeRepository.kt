package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskader
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object InMemoryYrkesskadeRepository : YrkesskadeRepository {

    private val memory = ConcurrentHashMap<BehandlingId, YrkesskadeGrunnlag>()
    private val idSeq = AtomicLong(1)

    override fun hentHvisEksisterer(behandlingId: BehandlingId): YrkesskadeGrunnlag? =
        memory[behandlingId]

    override fun lagre(behandlingId: BehandlingId, registerYrkesskader: Yrkesskader?, oppgittYrkesskadeISøknad: Boolean?) {
        if (registerYrkesskader != null) {
            val existing = memory[behandlingId]
            memory[behandlingId] = YrkesskadeGrunnlag(
                id = existing?.id ?: idSeq.getAndIncrement(),
                behandlingId = behandlingId,
                yrkesskader = registerYrkesskader,
                oppgittYrkesskadeISøknad = oppgittYrkesskadeISøknad,
            )
        } else {
            memory.remove(behandlingId)
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        memory[fraBehandling]?.let {
            memory[tilBehandling] = it.copy(
                id = idSeq.getAndIncrement(),
                behandlingId = tilBehandling,
            )
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        memory.remove(behandlingId)
    }
}
