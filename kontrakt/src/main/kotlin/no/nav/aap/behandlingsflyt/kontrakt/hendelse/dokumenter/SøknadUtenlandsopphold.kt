package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

/**
 * Søknad om å beholde AAP under opphold i utlandet (NAV 11-03.07).
 */
public sealed interface SøknadUtenlandsopphold : Melding

/**
 * @param mottattDato Datoen søknaden ble mottatt.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class SøknadUtenlandsoppholdV0(
    public val mottattDato: LocalDate,
) : SøknadUtenlandsopphold
