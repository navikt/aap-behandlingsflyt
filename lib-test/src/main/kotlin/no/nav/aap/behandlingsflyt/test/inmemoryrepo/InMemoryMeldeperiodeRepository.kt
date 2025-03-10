package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import java.util.concurrent.ConcurrentHashMap

object InMemoryMeldeperiodeRepository: MeldeperiodeRepository {
    private val meldeperioder = ConcurrentHashMap<BehandlingId, List<Periode>>()

    override fun hent(behandlingId: BehandlingId): List<Periode> {
        return meldeperioder[behandlingId]!!
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): List<Periode>? {
        return meldeperioder[behandlingId]
    }

    override fun lagre(behandlingId: BehandlingId, meldeperioder: List<Periode>) {
        this.meldeperioder[behandlingId] = meldeperioder
    }
}