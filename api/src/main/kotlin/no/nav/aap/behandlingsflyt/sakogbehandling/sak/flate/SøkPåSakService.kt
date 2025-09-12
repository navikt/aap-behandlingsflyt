package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.repository.RepositoryProvider

class SøkPåSakService(
    val repositoryProvider: RepositoryProvider
) {
    fun søkEtterSaker(søketekst: String): List<Sak> {
        val skalSøkePåSaksnummer = søketekst.length == 7
        val skalSøkePåIdent = søketekst.length == 11

        return if (skalSøkePåSaksnummer) {
            søkPåSaksnummer(Saksnummer(søketekst))
        } else if (skalSøkePåIdent) {
            søkPåPersonIdent(søketekst)
        } else {
            emptyList()
        }
    }

    private fun søkPåSaksnummer(saksnummer: Saksnummer): List<Sak> {
        val sakRepository = repositoryProvider.provide<SakRepository>()
        val sak = sakRepository.hentHvisFinnes(saksnummer)
        return listOfNotNull(sak)
    }

    private fun søkPåPersonIdent(personIdent: String): List<Sak> {
        val personRepository = repositoryProvider.provide<PersonRepository>()
        val sakRepository = repositoryProvider.provide<SakRepository>()
        val person = personRepository.finn(Ident(personIdent))
        if (person == null) {
            return emptyList()
        }
        return sakRepository.finnSakerFor(person)
    }
}