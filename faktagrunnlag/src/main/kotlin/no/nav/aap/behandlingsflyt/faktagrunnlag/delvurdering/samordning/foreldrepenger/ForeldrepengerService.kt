package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.foreldrepenger

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.flyt.FlytKontekst
import java.time.LocalDate

class ForeldrepengerService(
    connection: DBConnection
): Informasjonskrav {
    private val fpGateway = ForeldrepengerGateway()
    private val sakService = SakService(connection)
    private val foreldrepengerRepository = ForeldrepengerRepository(connection)

    private fun hentYtelseForeldrepenger(personIdent: String, fom: LocalDate, tom: LocalDate): ForeldrepengerResponse {
        return fpGateway.hentVedtakYtelseForPerson(
            ForeldrepengerRequest(
                Aktør(personIdent),
                Periode(fom, tom)
            )
        )
    }

    override fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean {
        val sak = sakService.hent(kontekst.sakId)
        val personIdent = sak.person.aktivIdent().identifikator

        val foreldrepenger = hentYtelseForeldrepenger(personIdent, sak.rettighetsperiode.fom, sak.rettighetsperiode.tom)
        /*
        TODO: Implementer ellers krevd lagringslogikk her
        foreldrepengerRepository.lagre(foreldrepenger)
        */
        return true
    }

    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): ForeldrepengerService {
            return ForeldrepengerService(connection)
        }
    }
}