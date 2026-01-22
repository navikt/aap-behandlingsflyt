package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository
import java.time.LocalDate

interface UnderveisRepository : Repository {
    fun hent(behandlingId: BehandlingId): UnderveisGrunnlag
    fun hentHvisEksisterer(behandlingId: BehandlingId): UnderveisGrunnlag?
    fun lagre(
        behandlingId: BehandlingId,
        underveisperioder: List<Underveisperiode>,
        input: Faktagrunnlag
    )
    fun hentSakerMedSisteUnderveisperiodeFørDato(sisteUnderveisDato: LocalDate): Set<SakId>
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}
