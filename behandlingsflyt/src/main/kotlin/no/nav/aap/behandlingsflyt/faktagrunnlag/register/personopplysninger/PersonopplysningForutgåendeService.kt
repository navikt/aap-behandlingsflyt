package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import com.papsign.ktor.openapigen.route.info
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter.PdlPersonopplysningGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class PersonopplysningForutgåendeService private constructor(
    private val sakService: SakService,
    private val personopplysningForutgåendeRepository: PersonopplysningForutgåendeRepository,
    private val personopplysningGateway: PersonopplysningGateway,
) : Informasjonskrav {
    val logger = LoggerFactory.getLogger(PersonopplysningForutgåendeService::class.java)

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val sak = sakService.hent(kontekst.sakId)

        try {
            val personopplysninger = personopplysningGateway.innhentMedHistorikk(sak.person) ?: error("fødselsdato skal alltid eksistere i PDL")
            logger.info("hentet personopplysninger: $personopplysninger")
            val eksisterendeData = personopplysningForutgåendeRepository.hentHvisEksisterer(kontekst.behandlingId)

            if (personopplysninger != eksisterendeData?.brukerPersonopplysning) {
                personopplysningForutgåendeRepository.lagre(kontekst.behandlingId, personopplysninger)
                val nyeData = personopplysningForutgåendeRepository.hentHvisEksisterer(kontekst.behandlingId)
                logger.info("hentet nye data fra PDL: $nyeData")
                return IKKE_ENDRET//return ENDRET
            }

        } catch (e: Exception) {
            logger.info("feilet ved innhenting PDL: ${e.message}, stack: $e ")
        }

        return IKKE_ENDRET
    }

    companion object : Informasjonskravkonstruktør {
        override fun erRelevant(kontekst: FlytKontekstMedPerioder): Boolean {
            // Skal alltid innhente ferske opplysninger
            return true
        }

        override fun konstruer(connection: DBConnection): PersonopplysningForutgåendeService {
            val repositoryProvider = RepositoryProvider(connection)
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val personopplysningRepository = repositoryProvider.provide<PersonopplysningForutgåendeRepository>()
            return PersonopplysningForutgåendeService(
                SakService(sakRepository),
                personopplysningRepository,
                PdlPersonopplysningGateway
            )
        }
    }
}
