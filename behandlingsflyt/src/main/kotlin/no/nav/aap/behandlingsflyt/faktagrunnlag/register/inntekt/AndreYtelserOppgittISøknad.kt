package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AndreUtbetalingerDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AndreUtbetalingerYtelserDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.JaNei
import no.nav.aap.misc.inntekt.AndreUtbetalingerYtelser
import no.nav.aap.misc.inntekt.AndreYtelserSøknad


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