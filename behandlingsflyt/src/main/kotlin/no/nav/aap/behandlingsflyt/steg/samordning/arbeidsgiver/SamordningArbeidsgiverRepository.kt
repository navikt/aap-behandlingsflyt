package no.nav.aap.behandlingsflyt.steg.samordning.arbeidsgiver

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.samordning.arbeidsgiver.SamordningArbeidsgiverGrunnlag
import no.nav.aap.samordning.arbeidsgiver.SamordningArbeidsgiverVurdering

interface SamordningArbeidsgiverRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningArbeidsgiverGrunnlag?
    fun lagre(sakId: SakId, behandlingId: BehandlingId, refusjonkravVurderinger: SamordningArbeidsgiverVurdering)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}
