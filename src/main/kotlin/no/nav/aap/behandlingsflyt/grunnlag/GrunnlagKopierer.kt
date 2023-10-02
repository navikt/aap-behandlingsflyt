package no.nav.aap.behandlingsflyt.grunnlag

import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import no.nav.aap.behandlingsflyt.grunnlag.person.PersoninformasjonTjeneste
import no.nav.aap.behandlingsflyt.grunnlag.sykdom.SykdomsTjeneste
import no.nav.aap.behandlingsflyt.grunnlag.yrkesskade.YrkesskadeTjeneste

object GrunnlagKopierer {

    fun overfør(fraBehandling: Behandling, tilBehandling: Behandling) {
        PersoninformasjonTjeneste.kopier(fraBehandling, tilBehandling)
        YrkesskadeTjeneste.kopier(fraBehandling, tilBehandling)
        SykdomsTjeneste.kopier(fraBehandling, tilBehandling)
    }
}
