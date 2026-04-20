package no.nav.aap.behandlingsflyt.behandling.fraværfastsattaktivitet

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderAktivitetsplikt11_8Service(
    val meldekortRepository: MeldekortRepository
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        meldekortRepository = repositoryProvider.provide()
    )

    fun foo(behandlingId: BehandlingId){
        val meldekortGrunnlag = meldekortRepository.hentHvisEksisterer(behandlingId)
        val resultat = vurder(meldekortGrunnlag)
        lagre(resultat)
    }


    fun vurder(){}

    fun lagre(){}
}