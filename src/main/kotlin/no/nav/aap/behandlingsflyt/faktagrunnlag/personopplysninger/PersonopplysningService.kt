package no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger

import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.adapter.PersonRegisterMock
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sak.SakService
import no.nav.aap.behandlingsflyt.sak.PersonRepository

class PersonopplysningService : Grunnlag {

    override fun oppdater(transaksjonsconnection: DBConnection, kontekst: FlytKontekst): Boolean {
        val sakService = SakService(transaksjonsconnection)
        val sak = sakService.hent(kontekst.sakId)

        val personopplysninger = PersonRegisterMock.innhent(sak.person.identer())
        if (personopplysninger.size != 1) {
            throw IllegalStateException("fant flere personer enn forventet")
        }

        val behandlingId = kontekst.behandlingId
        val gamleData = PersoninformasjonRepository.hentHvisEksisterer(behandlingId)

        PersoninformasjonRepository.lagre(behandlingId, personopplysninger.first())
        val nyeData = PersoninformasjonRepository.hentHvisEksisterer(behandlingId)

        return nyeData == gamleData
    }
}
