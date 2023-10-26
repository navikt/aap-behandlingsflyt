package no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger

import no.nav.aap.behandlingsflyt.dbstuff.DbConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sak.SakService
import no.nav.aap.behandlingsflyt.sak.person.PersonRepository

class PersonopplysningService : Grunnlag {

    override fun oppdater(transaksjonsconnection: DbConnection, kontekst: FlytKontekst): Boolean {
        val sakService = SakService(transaksjonsconnection)
        val sak = sakService.hent(kontekst.sakId)
        //TODO: PersonService bør være unødvendig da vi kan hente identene fra sak
        val person = PersonRepository(transaksjonsconnection).hent(sak.person.identifikator)
        val behandlingId = kontekst.behandlingId

        val gamleData = PersoninformasjonRepository.hentHvisEksisterer(behandlingId)

        val personopplysninger = PersonRegisterMock.innhent(person.identer())
        if (personopplysninger.size != 1) {
            throw IllegalStateException("fant flere personer enn forventet")
        }

        PersoninformasjonRepository.lagre(behandlingId, personopplysninger.first())
        val nyeData = PersoninformasjonRepository.hentHvisEksisterer(behandlingId)

        return nyeData == gamleData
    }
}
