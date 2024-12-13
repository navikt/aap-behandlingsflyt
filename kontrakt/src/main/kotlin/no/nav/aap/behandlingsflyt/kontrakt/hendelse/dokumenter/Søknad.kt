package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

public sealed interface Søknad : Melding

/**
 * Dagens søknad-objekt. Ved inkompatibel endring, lag ny versjon.
 *
 * @param student Hvis ikke oppgitt, skal dette objektet være null.
 * @param yrkesskade Lovlig verdi er "ja/jA/Ja/JA". Alt annet blir tolket som false.
 * @param oppgitteBarn Om barn er oppgitt, mengden av identer.
 */
public data class SøknadV0(
    public val student: SøknadStudentDto?,
    public val yrkesskade: String,
    public val oppgitteBarn: OppgitteBarn?
) : Søknad

/**
 * @param erStudent Lovlig verdier er JA, AVBRUTT, NEI.
 * @param kommeTilbake Lovlige verdier er JA, NEI, VET_IKKE, IKKE_OPPGITT.
 */
public data class SøknadStudentDto(
    public val erStudent: String,
    public val kommeTilbake: String? = null
)

public data class OppgitteBarn(public val identer: Set<Ident>)

public data class Ident(val identifikator: String) {
    init {
        require(identifikator.matches("\\d{11}".toRegex())) { "Ugyldig identifikator" }
    }
}