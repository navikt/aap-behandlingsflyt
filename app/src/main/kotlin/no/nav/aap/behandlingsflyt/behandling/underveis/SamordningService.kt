package no.nav.aap.behandlingsflyt.behandling.underveis
/*
* Pleiepenger for sykt barn - team sykdom i familien
* Pleiepenger for nærstående - team sykdom i familien
* Omsorgspenger? - team sykdom i familien
* Svangerskapspenger - team foreldrepenger
* Foreldrepenger - team foreldrepenger
* Sykepenger? - team sykdom (På vent fra Øyvind, har potensielt 2 apier, bør klareres)
*
*
*
* Informasjonselementer fra ytelsene:
* Perioder med ytelse
* utbetalingsgrad per periode

 */

class SamordningService {

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
}