package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.arena.ArenaMigreringsdata
import no.nav.aap.behandlingsflyt.arena.ArenaMigreringsdataRepository
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

object InMemoryArenaMigreringsdataRepository : ArenaMigreringsdataRepository {
    private val aktive = ConcurrentHashMap<Pair<BehandlingId, StegType>, ArenaMigreringsdata>()

    override fun lagre(behandlingId: BehandlingId, steg: StegType, data: Any, hentetTidspunkt: Instant) {
        val json = DefaultJsonMapper.toJson(data)
        if (aktive[behandlingId to steg]?.data == json) return
        aktive[behandlingId to steg] = ArenaMigreringsdata(
            behandlingId = behandlingId,
            steg = steg,
            hentetTidspunkt = hentetTidspunkt,
            data = json,
        )
    }

    override fun hentAktivHvisEksisterer(behandlingId: BehandlingId, steg: StegType): ArenaMigreringsdata? {
        return aktive[behandlingId to steg]
    }

    fun reset() {
        aktive.clear()
    }
}
