package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AndreUtbetalingerDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AndreUtbetalingerYtelserDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.JaNei

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
    STIPEND_FRA_LÅNEKASSEN, //Sykestipend
    LÅN_FRA_LÅNEKASSEN,
    INGEN_AV_DISSE;
}


private fun mapYtelseEnum(eksternType: AndreUtbetalingerYtelserDto): AndreUtbetalingerYtelser {
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

    val lønn = when (ytelser.lønn) {
        JaNei.Ja -> true
        JaNei.Nei -> false
        null -> null
    }

    return AndreYtelserSøknad(
        afpKilder = ytelser.afp?.hvemBetaler,
        ekstraLønn = lønn,
        stønad = ytelser.stønad?.map { mapYtelseEnum(it) }

    )
}