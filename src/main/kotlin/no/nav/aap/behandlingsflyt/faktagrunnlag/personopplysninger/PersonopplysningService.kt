package no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger

import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.adapter.PersonRegisterMock
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sak.SakService

class PersonopplysningService : Grunnlag {

    override fun oppdater(transaksjonsconnection: DBConnection, kontekst: FlytKontekst): Boolean {
        val personopplysningRepository = PersonopplysningRepository(transaksjonsconnection)
        val sakService = SakService(transaksjonsconnection)
        val sak = sakService.hent(kontekst.sakId)

        val personopplysninger = PersonRegisterMock.innhent(sak.person.identer())
        if (personopplysninger.size != 1) {
            throw IllegalStateException("fant flere personer enn forventet")
        }

        val behandlingId = kontekst.behandlingId
        val gamleData = personopplysningRepository.hentHvisEksisterer(behandlingId)

        personopplysningRepository.lagre(behandlingId, personopplysninger.first())
        val nyeData = personopplysningRepository.hentHvisEksisterer(behandlingId)

        return nyeData == gamleData
    }
}
