package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

public sealed interface Søknad : Melding

/**
 * Dagens søknad-objekt. Ved inkompatibel endring, lag ny versjon.
 */
public data class SøknadV0(
    public val student: SøknadStudentDto,
    public val yrkesskade: String,
    public val oppgitteBarn: OppgitteBarn?
) : Søknad

public data class SøknadV1(
    public val student: SøknadStudentDto,
    public val yrkesskade: String,
    public val oppgitteBarn: OppgitteBarn?
) : Søknad

public data class SøknadStudentDto(
    public val erStudent: String,
    public val kommeTilbake: String? = null
)

public data class OppgitteBarn(public val identer: Set<String>)