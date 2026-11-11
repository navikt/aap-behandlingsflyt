package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object InMemoryMedlemskapUnntakRepository : MedlemskapUnntakRepository {
    private data class Grunnlag(val id: Long, val unntak: MedlemskapUnntakGrunnlag)

    private val grunnlag = ConcurrentHashMap<BehandlingId, List<Grunnlag>>()
    private val nesteId = AtomicLong()

    override fun lagreUnntakMedlemskap(
        behandlingId: BehandlingId,
        medlemskapUnntakGrunnlag: MedlemskapUnntakGrunnlag
    ): Long {
        val nytt = Grunnlag(nesteId.incrementAndGet(), medlemskapUnntakGrunnlag.copy(unntak = medlemskapUnntakGrunnlag.unntak.toList()))
        grunnlag.compute(behandlingId) { _, historikk -> historikk.orEmpty() + nytt }
        return nytt.id
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): MedlemskapUnntakGrunnlag? =
        grunnlag[behandlingId]?.lastOrNull()?.unntak

    internal fun hent(id: Long): MedlemskapUnntakGrunnlag =
        grunnlag.values.asSequence().flatten().first { it.id == id }.unntak

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        grunnlag[fraBehandling]?.lastOrNull()?.let { grunnlag[tilBehandling] = listOf(it) }
    }

    override fun slett(behandlingId: BehandlingId) {
        grunnlag.remove(behandlingId)
    }
}
