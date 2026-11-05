package no.nav.aap.utenlandsopphold

import no.nav.aap.komponenter.type.Periode
import java.time.LocalDateTime
import java.util.Locale

enum class SøknadUtenlandsoppholdResultat {
    INNVILGET,
    DELVIS_INNVILGET,
    AVSLÅTT,
}

data class SøknadUtenlandsoppholdVurdering(
    val søknadsperiode: Periode,
    val land: String,
    val resultat: SøknadUtenlandsoppholdResultat,
    val godkjentPeriode: Periode?,
    val antallGodkjenteDager: Int?,
    val begrunnelse: String,
    val vurdertAv: String,
    val vurdertTidspunkt: LocalDateTime,
) {
    init {
        val feil = valider(søknadsperiode, land, resultat, godkjentPeriode, antallGodkjenteDager, begrunnelse)
        require(feil == null) { feil!! }
    }

    companion object {
        private val gyldigeLandkoder: Set<String> = Locale.getISOCountries().toSet()

        /** Returnerer en feilmelding til saksbehandler, eller null hvis vurderingen er gyldig. */
        fun valider(
            søknadsperiode: Periode,
            land: String,
            resultat: SøknadUtenlandsoppholdResultat,
            godkjentPeriode: Periode?,
            antallGodkjenteDager: Int?,
            begrunnelse: String,
        ): String? {
            if (land !in gyldigeLandkoder) {
                return "Land må oppgis som en gyldig landkode (ISO 3166-1 alfa-2)."
            }
            if (begrunnelse.isBlank()) {
                return "Begrunnelse må fylles ut."
            }
            if (antallGodkjenteDager != null && antallGodkjenteDager <= 0) {
                return "Antall godkjente dager må være større enn 0."
            }
            if (godkjentPeriode != null && !søknadsperiode.inneholder(godkjentPeriode)) {
                return "Godkjent periode må ligge innenfor søknadsperioden."
            }
            return when (resultat) {
                SøknadUtenlandsoppholdResultat.AVSLÅTT ->
                    if (godkjentPeriode != null || antallGodkjenteDager != null) {
                        "Godkjent periode og antall godkjente dager kan ikke oppgis ved avslag."
                    } else null

                SøknadUtenlandsoppholdResultat.INNVILGET,
                SøknadUtenlandsoppholdResultat.DELVIS_INNVILGET ->
                    if (godkjentPeriode == null && antallGodkjenteDager == null) {
                        "Godkjent periode eller antall godkjente dager må oppgis ved innvilgelse."
                    } else null
            }
        }
    }
}

data class SøknadUtenlandsoppholdGrunnlag(
    val vurdering: SøknadUtenlandsoppholdVurdering,
)
