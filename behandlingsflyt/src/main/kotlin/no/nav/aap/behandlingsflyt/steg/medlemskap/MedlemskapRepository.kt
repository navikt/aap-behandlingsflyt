package no.nav.aap.behandlingsflyt.steg.medlemskap

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.medlemskap.MedlemskapDataIntern
import no.nav.aap.medlemskap.MedlemskapUnntakGrunnlag

interface MedlemskapRepository : Repository {
    fun lagreUnntakMedlemskap(behandlingId: BehandlingId, unntak: List<MedlemskapDataIntern>): Long
    fun hentHvisEksisterer(behandlingId: BehandlingId): MedlemskapUnntakGrunnlag?
}