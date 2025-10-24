package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.andreYtelserOppgittISøknad

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Ytelser
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AndreUtbetalingerYtelser


/**
 * Enumene kommer fra alternativer som er svart fra søknaden. Disse er:
 *
 * Godtgjørelse eller lønn for verv
 * Økonomisk sosialhjelp
 * Omsorgsstønad (tidligere omsorgslønn)
 * Introduksjonsstønad (introduksjonsprogrammet)
 * Kvalifiseringsstønad (kvalifiseringsprogrammet)
 * Ytelser fra utenlandske trygdemyndigheter
 * Avtalefestet pensjon (AFP)
 * Lån fra Lånekassen
 * Sykestipend fra Lånekassen
 * Ingen av disse
 */


public enum class YtelseOppgittISøknadDto{
    ØKONOMISK_SOSIALHJELP,
    OMSORGSSTØNAD,
    INTRODUKSJONSSTØNAD,
    KVALIFISERINGSSTØNAD,
    VERV,
    UTLAND,
    AFP,
    STIPEND,
    LÅN,
    NEI;
    public companion object {
        public fun fromDb(value: String): AndreUtbetalingerYtelser = value.uppercase().let {
            try {
                AndreUtbetalingerYtelser.valueOf(it)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Unknown YtelseType: $value")
            }
        }
    }
    }



data class AndreYtelserOppgittISøknadDto (
    val ytelserOppgittISøknad : List<YtelseOppgittISøknadDto>? = null,
    val lønn: Boolean? = null,
    val
)

