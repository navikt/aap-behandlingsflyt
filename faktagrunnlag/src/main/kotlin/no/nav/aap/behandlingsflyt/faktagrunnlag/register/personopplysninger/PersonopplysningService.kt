package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlagkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter.PdlPersonopplysningGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.verdityper.flyt.FlytKontekst

class PersonopplysningService private constructor(
    private val connection: DBConnection,
    private val personopplysningGateway: PersonopplysningGateway,
) : Grunnlag {

    companion object : Grunnlagkonstruktør {
        override fun konstruer(connection: DBConnection): PersonopplysningService {
            return PersonopplysningService(
                connection,
                PdlPersonopplysningGateway
            )
        }
    }

    override fun oppdater(kontekst: FlytKontekst): Boolean {
        val personopplysningRepository = PersonopplysningRepository(connection)
        val sakService = SakService(connection)
        val sak = sakService.hent(kontekst.sakId)

        val eksisterendeData = personopplysningRepository.hentHvisEksisterer(kontekst.behandlingId)
        val skalInnhente = eksisterendeData?.personopplysning?.skalInnhentes() ?: true

        if (skalInnhente) {
            val personopplysninger = runBlocking {
                personopplysningGateway.innhent(sak.person) ?: error("fødselsdato skal alltid eksistere i PDL")
            }

            if (personopplysninger != eksisterendeData?.personopplysning) {
                personopplysningRepository.lagre(kontekst.behandlingId, personopplysninger)
                return true
            }
        }
        return false
    }
}
