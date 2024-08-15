package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter.PdlPersonopplysningGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.verdityper.flyt.FlytKontekst

class PersonopplysningService private constructor(
    private val sakService: SakService,
    private val personopplysningRepository: PersonopplysningRepository,
    private val personopplysningGateway: PersonopplysningGateway,
) : Informasjonskrav {

    override fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean {
        val sak = sakService.hent(kontekst.sakId)
        val personopplysninger = personopplysningGateway.innhent(sak.person) ?: error("fødselsdato skal alltid eksistere i PDL")
        val eksisterendeData = personopplysningRepository.hentHvisEksisterer(kontekst.behandlingId)

        // TODO: Oppdatere person tabellen med identene til bruker for å detektere splitt / merge og utlede behovet for å feile
        if (personopplysninger != eksisterendeData?.brukerPersonopplysning) {
            personopplysningRepository.lagre(kontekst.behandlingId, personopplysninger)
            return false
        }
        return true
    }

    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): PersonopplysningService {
            return PersonopplysningService(
                SakService(connection),
                PersonopplysningRepository(connection),
                PdlPersonopplysningGateway
            )
        }
    }
}
