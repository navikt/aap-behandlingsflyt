package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AndreUtbetalingerDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AndreUtbetalingerYtelserDto




/**
 * Lønn I Søknad svarer på spørsmålet "Har du fått eller skal du få ekstra utbetalinger fra arbeidsgiver"
 * Dette kan for eksempel være sluttpakke, etterlønn eller andre goder.
 * Du skal svare “nei” hvis du bare mottar vanlig lønn eller feriepenger.
 *
 */

/**
 * @property ekstraLønn Kan være ekstra utbetalinger som sluttpakke, etterlønn ellr andre goder. Ikke vanlig lønn eller feriepenger
 * @property afpKilder Kilde til AFP
 */
data class AndreYtelserSøknad(
    val ekstraLønn: Boolean?,
    val afpKilder: String? = null,
    val stønad: List<AndreUtbetalingerYtelser>?
)

enum class AndreUtbetalingerYtelser {
    ØKONOMISK_SOSIALHJELP,
    OMSORGSSTØNAD,
    INTRODUKSJONSSTØNAD,
    KVALIFISERINGSSTØNAD,
    GODGJØRELSE_ELLER_LØNN_FRA_VERV,
    YTELSE_FRA_UTENLANDSKE_TRYGDEMYNDIGHETER,
    AFP,
    STIPEND_FRA_LÅNEKASSEN,
    LÅN_FRA_LÅNEKASSEN,
    INGEN_AV_DISSE;

    public companion object {
        public fun fromDb(value: String): AndreUtbetalingerYtelser = value.uppercase().let {
            try {
                valueOf(it)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Unknown YtelseType: $value")
            }
        }
    }
}


private fun mapYtelseEnum(eksternType : AndreUtbetalingerYtelserDto): AndreUtbetalingerYtelser {
    return when (eksternType) {
        AndreUtbetalingerYtelserDto.OMSORGSSTØNAD -> AndreUtbetalingerYtelser.OMSORGSSTØNAD
        AndreUtbetalingerYtelserDto.ØKONOMISK_SOSIALHJELP -> AndreUtbetalingerYtelser.ØKONOMISK_SOSIALHJELP
        AndreUtbetalingerYtelserDto.KVALIFISERINGSSTØNAD -> AndreUtbetalingerYtelser.KVALIFISERINGSSTØNAD
        AndreUtbetalingerYtelserDto.AFP -> AndreUtbetalingerYtelser.AFP
        AndreUtbetalingerYtelserDto.INTRODUKSJONSSTØNAD -> AndreUtbetalingerYtelser.INTRODUKSJONSSTØNAD
        AndreUtbetalingerYtelserDto.LÅN -> AndreUtbetalingerYtelser.LÅN_FRA_LÅNEKASSEN
        AndreUtbetalingerYtelserDto.STIPEND -> AndreUtbetalingerYtelser.STIPEND_FRA_LÅNEKASSEN
        AndreUtbetalingerYtelserDto.UTLAND -> AndreUtbetalingerYtelser.YTELSE_FRA_UTENLANDSKE_TRYGDEMYNDIGHETER
        AndreUtbetalingerYtelserDto.VERV -> AndreUtbetalingerYtelser.GODGJØRELSE_ELLER_LØNN_FRA_VERV
        AndreUtbetalingerYtelserDto.NEI -> AndreUtbetalingerYtelser.INGEN_AV_DISSE
    }
}




fun mapOppgitteYtelser(ytelser: AndreUtbetalingerDto): AndreYtelserSøknad {
    return AndreYtelserSøknad(
        afpKilder = ytelser.afp,
        ekstraLønn = ytelser.lønn,
        stønad = ytelser.stønad?.map { mapYtelseEnum(it) }

    )
}