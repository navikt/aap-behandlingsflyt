package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.RegisterBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.VurderteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

object InMemoryBarnRepository : BarnRepository {
    private val barn = ConcurrentHashMap<BehandlingId, List<String>>()
    private val oppgitteBarn = ConcurrentHashMap<BehandlingId, OppgitteBarn>()
    private val registerBarn = ConcurrentHashMap<BehandlingId, List<Ident>>()
    private val vurdertBarn = ConcurrentHashMap<BehandlingId, List<VurdertBarn>>()
    private val lock = Object()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): BarnGrunnlag? {
        synchronized(lock) {
            return if (barn.containsKey(behandlingId) || oppgitteBarn.containsKey(behandlingId) ||
                registerBarn.containsKey(behandlingId) || vurdertBarn.containsKey(behandlingId)
            ) {
                BarnGrunnlag(
                    oppgitteBarn = oppgitteBarn[behandlingId],
                    registerbarn = registerBarn[behandlingId]?.let {
                        RegisterBarn(
                            id = 0,
                            identer = it.toList()
                        )
                    },
                    vurderteBarn = vurdertBarn[behandlingId]?.let {
                        VurderteBarn(
                            id = 0,
                            barn = it,
                            vurdertAv = "ingen",
                            vurdertTidspunkt = LocalDateTime.now()
                        )
                    }
                )
            } else {
                null
            }
        }
    }

    override fun hent(behandlingId: BehandlingId): BarnGrunnlag {
        return hentHvisEksisterer(behandlingId)
            ?: BarnGrunnlag(null, null, null)
    }

    override fun lagreOppgitteBarn(
        behandlingId: BehandlingId,
        oppgitteBarn: OppgitteBarn?
    ) {
        synchronized(lock) {
            if (oppgitteBarn != null) {
                this.oppgitteBarn[behandlingId] = oppgitteBarn
            } else {
                this.oppgitteBarn.remove(behandlingId)
            }
        }
    }

    override fun lagreRegisterBarn(
        behandlingId: BehandlingId,
        barn: Set<Ident>
    ) {
        synchronized(lock) {
            if (barn.isNotEmpty()) {
                registerBarn[behandlingId] = barn.toList()
            } else {
                registerBarn.remove(behandlingId)
            }
        }
    }

    override fun lagreVurderinger(
        behandlingId: BehandlingId,
        vurdertAv: String,
        vurderteBarn: List<VurdertBarn>
    ) {
        synchronized(lock) {
            if (vurderteBarn.isNotEmpty()) {
                this.vurdertBarn[behandlingId] = vurderteBarn
            } else {
                this.vurdertBarn.remove(behandlingId)
            }
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        TODO("Not yet implemented")
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(lock) {
            barn.remove(behandlingId)
            oppgitteBarn.remove(behandlingId)
            registerBarn.remove(behandlingId)
            vurdertBarn.remove(behandlingId)
        }
    }
}