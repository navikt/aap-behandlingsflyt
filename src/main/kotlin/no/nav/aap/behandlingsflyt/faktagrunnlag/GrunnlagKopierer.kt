package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import no.nav.aap.behandlingsflyt.faktagrunnlag.bistand.BistandsRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.PersoninformasjonRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomsRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.db.InMemoryStudentRepository

object GrunnlagKopierer {

    fun overfør(fraBehandling: Behandling, tilBehandling: Behandling) {
        PersoninformasjonRepository.kopier(fraBehandling, tilBehandling)
        YrkesskadeRepository.kopier(fraBehandling, tilBehandling)
        SykdomsRepository.kopier(fraBehandling, tilBehandling)
        InMemoryStudentRepository.kopier(fraBehandling, tilBehandling)
        BistandsRepository.kopier(fraBehandling, tilBehandling)
        SykepengerErstatningRepository.kopier(fraBehandling, tilBehandling)
    }
}
