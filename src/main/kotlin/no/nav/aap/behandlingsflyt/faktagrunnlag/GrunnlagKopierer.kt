package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import no.nav.aap.behandlingsflyt.faktagrunnlag.bistand.BistandsTjeneste
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.PersoninformasjonRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomsTjeneste
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykepengerErstatningTjeneste
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.db.InMemoryStudentRepository

object GrunnlagKopierer {

    fun overfør(fraBehandling: Behandling, tilBehandling: Behandling) {
        PersoninformasjonRepository.kopier(fraBehandling, tilBehandling)
        YrkesskadeRepository.kopier(fraBehandling, tilBehandling)
        SykdomsTjeneste.kopier(fraBehandling, tilBehandling)
        InMemoryStudentRepository.kopier(fraBehandling, tilBehandling)
        BistandsTjeneste.kopier(fraBehandling, tilBehandling)
        SykepengerErstatningTjeneste.kopier(fraBehandling, tilBehandling)
    }
}
