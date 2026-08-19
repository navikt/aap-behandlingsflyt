package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object InMemoryUnderveisRepository : UnderveisRepository {
    private val grunnlag = ConcurrentHashMap<BehandlingId, UnderveisGrunnlag>()
    private val id = AtomicLong(0)
    private val ubesvartePerioder = ConcurrentHashMap<SakId, List<Periode>>()

    fun settUbesvarte(sakId: SakId, perioder: List<Periode>) {
        ubesvartePerioder[sakId] = perioder
    }

    override fun hent(behandlingId: BehandlingId): UnderveisGrunnlag {
        return hentHvisEksisterer(behandlingId)!!
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): UnderveisGrunnlag? {
        return grunnlag[behandlingId]
    }

    override fun hentBulk(behandlingIds: List<BehandlingId>): Map<BehandlingId, UnderveisGrunnlag> {
        return grunnlag.filterKeys { it in behandlingIds }
    }

    override fun lagre(behandlingId: BehandlingId, underveisperioder: List<Underveisperiode>, input: Faktagrunnlag) {
        grunnlag[behandlingId] = UnderveisGrunnlag(
            id = id.getAndIncrement(),
            perioder = underveisperioder,
        )
    }

    override fun hentSakerForGRegulering(datoForGJustering: LocalDate, nyttGrunnbeløp: Beløp): Set<SakId> {
        TODO("Not yet implemented")
    }

    override fun hentSakerMedSisteUnderveisperiodeFørDato(sisteUnderveisDato: LocalDate): Set<SakId> {
        TODO("Not yet implemented")
    }

    override fun hentUbesvarteMeldeperioderForDollyJobb(sakIds: List<SakId>, idag: LocalDate): Map<SakId, List<Periode>> {
        return ubesvartePerioder.filterKeys { it in sakIds }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        TODO("Not yet implemented")
    }

    override fun slett(behandlingId: BehandlingId) {
        grunnlag.remove(behandlingId)
    }
}