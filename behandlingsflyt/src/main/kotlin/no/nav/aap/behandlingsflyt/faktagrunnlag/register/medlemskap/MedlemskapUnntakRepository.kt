package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface MedlemskapUnntakRepository : Repository {
    fun lagreUnntakMedlemskap(behandlingId: BehandlingId, medlemskapUnntakGrunnlag: MedlemskapUnntakGrunnlag): Long
    fun hentHvisEksisterer(behandlingId: BehandlingId): MedlemskapUnntakGrunnlag?
}