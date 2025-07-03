package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsgiver

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository

    interface SamordningArbeidsgiverRepository: Repository {
        fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningArbeidsgiverVurdering?
        fun hentAlleVurderingerPåSak(sakId: SakId): List<SamordningArbeidsgiverVurdering>
        fun lagre(sakId: SakId, behandlingId: BehandlingId, refusjonkravVurderinger: SamordningArbeidsgiverVurdering)
        override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    }
