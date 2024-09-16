package no.nav.aap.behandlingsflyt.behandling.samordning

import no.nav.aap.behandlingsflyt.behandling.underveis.foreldrepenger.Aktør
import no.nav.aap.behandlingsflyt.behandling.underveis.foreldrepenger.ForeldrepengerGateway
import no.nav.aap.behandlingsflyt.behandling.underveis.foreldrepenger.ForeldrepengerRequest
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.SamordningRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.sykepenger.SykepengerRequest
import no.nav.aap.behandlingsflyt.behandling.underveis.sykepenger.SykepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
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
    connection: DBConnection
): Informasjonskrav {
    private val fpGateway = ForeldrepengerGateway()
    private val spGateway = SykepengerGateway()
    private val sakService = SakService(connection)

    private val regelset = listOf(
        SamordningRegel()
    )

    private fun hentYtelseForeldrepenger(personIdent: String, fom: LocalDate, tom: LocalDate): Tidslinje<SamordningGradering> {
        val fpResponse = fpGateway.hentVedtakYtelseForPerson(
            ForeldrepengerRequest(
                Aktør(personIdent),
                Periode(fom, tom)
            ))


        /*
        Todo: Implementer tidslinjer, må avklares
        return Tidslinje(listOf(Segment(

        )))*/

        return Tidslinje()
    }

    private fun hentYtelseSykepenger(personIdent: String, fom: LocalDate, tom: LocalDate): Tidslinje<SamordningGradering> {
        val spResponse =  spGateway.hentYtelseSykepenger(
            SykepengerRequest(
                setOf(personIdent),
                fom,
                tom
            )
        )
        /*
        Todo: Implementer tidslinjer, må avklares
        return Tidslinje(listOf(Segment(

        )))*/
        return Tidslinje()
    }

    override fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean {
        val sak = sakService.hent(kontekst.sakId)
        val personIdent = sak.person.aktivIdent().identifikator

        val sykepenger = hentYtelseSykepenger(personIdent, sak.rettighetsperiode.fom, sak.rettighetsperiode.tom)
        val foreldrepenger = hentYtelseForeldrepenger(personIdent, sak.rettighetsperiode.fom, sak.rettighetsperiode.tom)

        //TODO: Implementer repo lookups
        return false
/*
        val tidslinje = sykepenger.kombiner(
            foreldrepenger,
            JoinStyle.CROSS_JOIN) { periode, venstreSegment, høyreSegment ->
                val verdi = requireNotNull(venstreSegment).verdi
                if (høyreSegment != null) {
                    Segment(periode,)
                }
        }*/
    }

    fun vurder() : Tidslinje<SamordningGradering> {
        return Tidslinje()
    }

    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): SamordningService {
            return SamordningService(connection)
        }
    }
}