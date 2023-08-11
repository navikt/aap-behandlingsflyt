package no.nav.aap.domene.behandling.grunnlag

import no.nav.aap.domene.behandling.Behandling
import no.nav.aap.domene.behandling.grunnlag.person.PersoninformasjonTjeneste
import no.nav.aap.domene.behandling.grunnlag.yrkesskade.YrkesskadeTjeneste

object GrunnlagKopierer {

    fun overfør(fraBehandling: Behandling, tilBehandling: Behandling) {
        PersoninformasjonTjeneste.kopier(fraBehandling, tilBehandling)
        YrkesskadeTjeneste.kopier(fraBehandling, tilBehandling)
    }
}
