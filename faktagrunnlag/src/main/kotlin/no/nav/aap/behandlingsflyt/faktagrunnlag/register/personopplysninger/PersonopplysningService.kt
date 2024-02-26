package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlagkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter.PdlPersonopplysningGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.verdityper.flyt.FlytKontekst

class PersonopplysningService private constructor(
    private val sakService: SakService,
    private val personopplysningRepository: PersonopplysningRepository,
    private val personopplysningGateway: PersonopplysningGateway,
) : Grunnlag {

    override fun oppdater(kontekst: FlytKontekst): Boolean {
        val sak = sakService.hent(kontekst.sakId)
        val personopplysninger = personopplysningGateway.innhent(sak.person) ?: error("fødselsdato skal alltid eksistere i PDL")
        val eksisterendeData = personopplysningRepository.hentHvisEksisterer(kontekst.behandlingId)

        if (personopplysninger != eksisterendeData?.personopplysning) {
            personopplysningRepository.lagre(kontekst.behandlingId, personopplysninger)
            return true
        }
        return false
    }

    companion object : Grunnlagkonstruktør {
        override fun konstruer(connection: DBConnection): PersonopplysningService {
            return PersonopplysningService(
                SakService(connection),
                PersonopplysningRepository(connection),
                PdlPersonopplysningGateway
            )
        }
    }
}
