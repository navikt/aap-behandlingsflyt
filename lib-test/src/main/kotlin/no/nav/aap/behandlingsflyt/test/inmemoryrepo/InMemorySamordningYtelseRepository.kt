package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemorySamordningYtelseRepository : SamordningYtelseRepository {
    private val ytelser = ConcurrentHashMap<BehandlingId, List<SamordningYtelse>>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningYtelseGrunnlag? {
        return SamordningYtelseGrunnlag(grunnlagId = 2, ytelser[behandlingId] ?: emptyList())
    }

    override fun lagre(behandlingId: BehandlingId, samordningYtelser: List<SamordningYtelse>) {
        ytelser[behandlingId] = samordningYtelser
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        TODO()
    }
}