package no.nav.aap.behandlingsflyt.behandling.samordning

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.verdityper.flyt.FlytKontekst

/*
* Pleiepenger for sykt barn - team sykdom i familien
* Pleiepenger for nærstående - team sykdom i familien
* Omsorgspenger? - team sykdom i familien
* Svangerskapspenger - team foreldrepenger
* Foreldrepenger - team foreldrepenger
* Sykepenger? - team sykdom (På vent fra Øyvind, har potensielt 2 apier, bør klareres)
*
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

class SamordningService: Informasjonskrav {

    fun hentYtelsePleiePengerSyktBarn() {}

    fun hentYtelsePleiePengerForNærFamilie() {}

    fun hentYtelseOmsorgsPenger() {}

    /**
     * Anses som full ytelse, inklusivt 80% uttak
     */
    fun hentYtelseForeldrePenger() {}

    /**
     * Fulle svangerskapspenger av deltidsstilling anses som en redusert ytelse
     */
    fun hentYtelseSvangerskapsPenger() {}

    /**
     * Fulle sykepenger av deltidsstilling anses som en redusert ytelse
     */
    fun hentYtelseSykePenger() {}

    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): SamordningService {
            return SamordningService()
        }
    }
    
    override fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean {
        return false 
    }
}