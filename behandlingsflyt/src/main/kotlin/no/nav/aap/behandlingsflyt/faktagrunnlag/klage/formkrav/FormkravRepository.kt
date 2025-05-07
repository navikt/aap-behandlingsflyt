package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository

interface FormkravRepository: Repository {
        fun hentHvisEksisterer(behandlingId: BehandlingId): FormkravGrunnlag?
        fun hentAlleVurderinger(sakId: SakId, behandlingId: BehandlingId): Set<FormkravVurdering>
        fun lagre(behandlingId: BehandlingId, formkravVurdering: FormkravVurdering)
        override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}