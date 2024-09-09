package no.nav.aap.behandlingsflyt.behandling.samordning

import no.nav.aap.behandlingsflyt.behandling.underveis.foreldrepenger.Aktør
import no.nav.aap.behandlingsflyt.behandling.underveis.foreldrepenger.ForeldrepengerGateway
import no.nav.aap.behandlingsflyt.behandling.underveis.foreldrepenger.ForeldrepengerRequest
import no.nav.aap.behandlingsflyt.behandling.underveis.sykepenger.SykepengerRequest
import no.nav.aap.behandlingsflyt.behandling.underveis.sykepenger.SykepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.flyt.FlytKontekst
import java.time.LocalDate

/*
* Informasjonselementer fra ytelsene:
* Perioder med ytelse
* utbetalingsgrad per periode
*
* -----Tables vi trenger?:------
* SamordningGrunnlag
* - bruk underveisGrunnlag table
* SamordningPerioder
* - bruk underveisPerioder table
* SamordningPeriode
* - id(pk) - gradering (smallint) - perioder_id(fk){samordningsperioder} - periode(daterange)
*
* YtelsesGradering
* - id(pk) - samordningsperiodeId(fk) - ytelse(string/enum) - gradering (smallint)
*/

class SamordningService(
    private val sakService: SakService,
    private val fpGateway: ForeldrepengerGateway,
    private val spGateway: SykepengerGateway
): Informasjonskrav {

    private fun hentYtelseForeldrepenger(personIdent: String, fom: LocalDate, tom: LocalDate) {
        val fpResponse = fpGateway.hentVedtakYtelseForPerson(
            ForeldrepengerRequest(
                Aktør(personIdent),
                Periode(fom, tom)
            )
        )
        // TODO: Gjør dette om til tidslinje
    }

    private fun hentYtelseSykepenger(personIdent: String, fom: LocalDate, tom: LocalDate) {
        val spResponse =  spGateway.hentYtelseSykepenger(
            SykepengerRequest(
                setOf(personIdent),
                fom,
                tom
            )
        )
        // TODO: Gjør dette om til tidslinje
    }

    override fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean {
        val sak = sakService.hent(kontekst.sakId)
        val personIdent = sak.person.aktivIdent().identifikator

        val spTidslinje = hentYtelseSykepenger(personIdent, sak.rettighetsperiode.fom, sak.rettighetsperiode.tom)
        val fpTidslinje = hentYtelseForeldrepenger(personIdent, sak.rettighetsperiode.fom, sak.rettighetsperiode.tom)

        return false 
    }

    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): SamordningService {
            return SamordningService(
                SakService(connection),
                ForeldrepengerGateway(),
                SykepengerGateway(),
            )
        }
    }
}