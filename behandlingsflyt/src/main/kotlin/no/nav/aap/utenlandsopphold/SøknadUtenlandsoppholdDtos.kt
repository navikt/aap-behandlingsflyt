package no.nav.aap.utenlandsopphold

import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.time.LocalDateTime

data class SøknadUtenlandsoppholdVurderingDto(
    val søknadsperiode: Periode,
    val land: String,
    val resultat: SøknadUtenlandsoppholdResultat,
    val godkjentPeriode: Periode? = null,
    val antallGodkjenteDager: Int? = null,
    val begrunnelse: String,
) {
    fun tilVurdering(vurdertAv: String, vurdertTidspunkt: LocalDateTime) = SøknadUtenlandsoppholdVurdering(
        søknadsperiode = søknadsperiode,
        land = land,
        resultat = resultat,
        godkjentPeriode = godkjentPeriode,
        antallGodkjenteDager = antallGodkjenteDager,
        begrunnelse = begrunnelse,
        vurdertAv = vurdertAv,
        vurdertTidspunkt = vurdertTidspunkt,
    )
}

data class BekreftBrevSendtDto(
    val brevSendt: Boolean,
)

/** Opplysninger om søknaden som lokalkontor trenger for å vurdere den. */
data class SøknadUtenlandsoppholdDokumentDto(
    val journalpostId: String,
    val mottattDato: LocalDate,
    val fritekst: String?,
)
