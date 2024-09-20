package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.sykepenger

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekst
import java.time.LocalDate

class SykepengerService(
    connection: DBConnection
): Informasjonskrav {
    private val spGateway = SykepengerGateway()
    private val sakService = SakService(connection)
    private val sykepengerRepository = SykepengerRepository(connection)

    private fun hentYtelseSykepenger(personIdent: String, fom: LocalDate, tom: LocalDate): SykepengerResponse {
        return spGateway.hentYtelseSykepenger(
            SykepengerRequest(
                setOf(personIdent),
                fom,
                tom
            )
        )
    }

    override fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean {
        val sak = sakService.hent(kontekst.sakId)
        val personIdent = sak.person.aktivIdent().identifikator

        val sykepenger = hentYtelseSykepenger(personIdent, sak.rettighetsperiode.fom, sak.rettighetsperiode.tom)
        /*
        TODO: Implementer ellers krevd lagringslogikk her
        sykepengerRepository.lagre(sykepenger)
        */
        return true
    }

    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): SykepengerService {
            return SykepengerService(connection)
        }
    }
}