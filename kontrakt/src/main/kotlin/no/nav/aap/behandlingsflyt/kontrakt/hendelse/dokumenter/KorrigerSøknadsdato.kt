package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

public sealed interface KorrigerSøknadsdato : Melding

@JsonIgnoreProperties(ignoreUnknown = true)
public data class KorrigerSøknadsdatoV0(
    public val begrunnelse: String,
) : KorrigerSøknadsdato
