package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface MedlemskapRepository : Repository {
    fun lagreUnntakMedlemskap(behandlingId: BehandlingId, unntak: List<MedlemskapDataIntern>): Long
    fun hentHvisEksisterer(behandlingId: BehandlingId): MedlemskapUnntakGrunnlag?
}