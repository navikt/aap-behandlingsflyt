package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.KanBehandlesAutomatiskVurdering
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.LovvalgDelvurderingerGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.LovvalgDelvurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDate

object InMemoryLovvalgDelvurderingRepository : LovvalgDelvurderingRepository {
    private val grunnlag = mutableMapOf<BehandlingId, List<Long>>()
    private val vurderinger = mutableMapOf<Long, LovvalgDelvurderingerGrunnlag>()
    private var nesteId = 0L
    private val lock = Any()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): LovvalgDelvurderingerGrunnlag? =
        synchronized(lock) {
            grunnlag[behandlingId]?.lastOrNull()?.let { vurderinger.getValue(it) }
        }

    override fun hentBasertPåDelvurderingId(delvurderingId: Long): LovvalgDelvurderingerGrunnlag? =
        synchronized(lock) { vurderinger[delvurderingId] }

    override fun lagre(
        behandlingId: BehandlingId,
        vurdering: KanBehandlesAutomatiskVurdering,
        referansedato: LocalDate
    ) {
        synchronized(lock) {
            val eksisterende = hentHvisEksisterer(behandlingId)
            if (eksisterende?.vurdering() == vurdering && eksisterende?.referansedato == referansedato) return

            val id = ++nesteId
            vurderinger[id] = LovvalgDelvurderingerGrunnlag(
                kanBehandlesAutomatisk = vurdering.kanBehandlesAutomatisk,
                tilhørighetVurdering = vurdering.tilhørighetVurdering,
                periode = vurdering.periode,
                referansedato = referansedato,
                delvurderingId = id
            )
            grunnlag[behandlingId] = grunnlag[behandlingId].orEmpty() + id
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(lock) {
            val slettedeIder = grunnlag.remove(behandlingId).orEmpty()
            val refererteIder = grunnlag.values.flatten().toSet()
            slettedeIder.filterNot { it in refererteIder }.forEach { vurderinger.remove(it) }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        synchronized(lock) {
            grunnlag[fraBehandling]?.lastOrNull()?.let {
                require(tilBehandling !in grunnlag) { "Målbehandlingen har allerede et grunnlag" }
                grunnlag[tilBehandling] = listOf(it)
            }
        }
    }
}
