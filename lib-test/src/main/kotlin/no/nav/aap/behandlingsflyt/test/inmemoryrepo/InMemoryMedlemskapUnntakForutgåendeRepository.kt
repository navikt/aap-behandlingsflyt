package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapDataIntern
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakForutgåendeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.tilDomene
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object InMemoryMedlemskapUnntakForutgåendeRepository : MedlemskapUnntakForutgåendeRepository {
    private data class Grunnlag(val id: Long, val unntak: MedlemskapUnntakGrunnlag)

    private val grunnlag = ConcurrentHashMap<BehandlingId, List<Grunnlag>>()
    private val nesteId = AtomicLong()

    override fun lagreUnntakMedlemskap(behandlingId: BehandlingId, unntak: List<MedlemskapDataIntern>): Long {
        val nytt = Grunnlag(nesteId.incrementAndGet(), MedlemskapUnntakGrunnlag(unntak.tilDomene()))
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
