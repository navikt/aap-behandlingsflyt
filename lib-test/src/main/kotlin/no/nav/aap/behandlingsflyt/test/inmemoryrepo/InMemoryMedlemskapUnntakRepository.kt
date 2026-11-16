package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryMedlemskapUnntakRepository : MedlemskapUnntakRepository {
    private val grunnlag = mutableMapOf<BehandlingId, List<Long>>()
    private val unntak = mutableMapOf<Long, MedlemskapUnntakGrunnlag>()
    private var nesteId = 0L
    private val lock = Any()

    override fun lagreUnntakMedlemskap(
        behandlingId: BehandlingId,
        medlemskapUnntakGrunnlag: MedlemskapUnntakGrunnlag
    ): Long {
        synchronized(lock) {
            val id = ++nesteId
            unntak[id] = medlemskapUnntakGrunnlag
            grunnlag[behandlingId] = grunnlag[behandlingId].orEmpty() + id
            return id
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): MedlemskapUnntakGrunnlag? =
        synchronized(lock) {
            grunnlag[behandlingId]?.lastOrNull()?.let { unntak.getValue(it) }
        }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling) { "Kan ikke kopiere medlemsskapgrunnlag til samme behandling" }
        synchronized(lock) {
            grunnlag[fraBehandling]?.lastOrNull()?.let { grunnlag[tilBehandling] = listOf(it) }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(lock) {
            val slettedeIder = grunnlag.remove(behandlingId).orEmpty()
            val refererteIder = grunnlag.values.flatten().toSet()
            slettedeIder.filterNot { it in refererteIder }.forEach { unntak.remove(it) }
        }
    }
}
