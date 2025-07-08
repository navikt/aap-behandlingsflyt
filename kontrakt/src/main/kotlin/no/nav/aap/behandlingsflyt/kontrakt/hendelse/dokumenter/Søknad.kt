package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

public sealed interface Søknad : Melding

/**
 * Dagens søknad-objekt. Ved inkompatibel endring, lag ny versjon.
 *
 * @param student Hvis ikke oppgitt, skal dette objektet være null.
 * @param yrkesskade Lovlig verdi er "ja/jA/Ja/JA". Alt annet blir tolket som false.
 * @param oppgitteBarn Om barn er oppgitt, mengden av identer.
 * @param medlemskap Søkers opphold i utland
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class SøknadV0(
    public val student: SøknadStudentDto?,
    public val yrkesskade: String,
    public val oppgitteBarn: OppgitteBarn?,
    public val medlemskap: SøknadMedlemskapDto? = null
) : Søknad

/**
 * @param erStudent Lovlig verdier er JA, AVBRUTT, NEI.
 * @param kommeTilbake Lovlige verdier er JA, NEI, VET_IKKE, IKKE_OPPGITT.
 */
public data class SøknadStudentDto(
    public val erStudent: String,
    public val kommeTilbake: String? = null
)

/**
 * @param harBoddINorgeSiste5År Lovlig verdi er "ja/jA/Ja/JA". Alt annet blir tolket som false.
 * @param harArbeidetINorgeSiste5År Lovlig verdi er "ja/jA/Ja/JA". Alt annet blir tolket som false.
 * @param arbeidetUtenforNorgeFørSykdom Lovlig verdi er "ja/jA/Ja/JA". Alt annet blir tolket som false.
 * @param iTilleggArbeidUtenforNorge Lovlig verdi er "ja/jA/Ja/JA". Alt annet blir tolket som false
 */
public data class SøknadMedlemskapDto(
    val harBoddINorgeSiste5År: String?,
    val harArbeidetINorgeSiste5År: String?,
    val arbeidetUtenforNorgeFørSykdom: String?,
    @param:JsonAlias("itilleggArbeidUtenforNorge") val iTilleggArbeidUtenforNorge: String?,
    val utenlandsOpphold: List<UtenlandsPeriodeDto>?
)

/**
 * @param iArbeid Lovlig verdi er "ja/jA/Ja/JA". Alt annet blir tolket som false.
 */
public data class UtenlandsPeriodeDto(
    val land: String?,
    val tilDato: LocalDate?,
    val fraDato: LocalDate?,
    @param:JsonAlias("iarbeid") val iArbeid: String?,
    val utenlandsId: String?,
    val tilDatoLocalDate: LocalDate?,
    val fraDatoLocalDate: LocalDate?
)

public data class OppgitteBarn(
    @Deprecated("Erstattes av 'barn' siden ident i en rekke tilfeller vil kunne mangle")
    public val identer: Set<Ident>,
    public val barn: List<ManueltOppgittBarn> = emptyList(),
)

public data class ManueltOppgittBarn(
    public val navn: String? = null,
    public val fødselsdato: LocalDate? = null,
    public val ident: Ident? = null,
    public val relasjon: Relasjon? = null,
) {
    public enum class Relasjon {
        FORELDER,
        FOSTERFORELDER,
    }
}

public data class Ident(val identifikator: String) {
    init {
        require(identifikator.matches("\\d{11}".toRegex())) { "Ugyldig identifikator" }
    }
}

