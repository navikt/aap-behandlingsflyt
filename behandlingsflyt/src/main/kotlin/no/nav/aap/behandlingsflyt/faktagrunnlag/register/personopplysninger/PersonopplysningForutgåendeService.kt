package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

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

class PersonopplysningForutgåendeService private constructor(
    private val sakService: SakService,
    private val personopplysningForutgåendeRepository: PersonopplysningForutgåendeRepository,
    private val personopplysningGateway: PersonopplysningGateway,
) : Informasjonskrav {

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val sak = sakService.hent(kontekst.sakId)
        val personopplysninger = personopplysningGateway.innhent(sak.person, true) ?: error("fødselsdato skal alltid eksistere i PDL")
        val eksisterendeData = personopplysningForutgåendeRepository.hentHvisEksisterer(kontekst.behandlingId)

        if (personopplysninger != eksisterendeData?.brukerPersonopplysning) {
            personopplysningForutgåendeRepository.lagre(kontekst.behandlingId, personopplysninger)
            return ENDRET
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
