package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AfpDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AndreUtbetalingerDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AndreUtbetalingerYtelserDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.JaNei

data class AndreUtbetalingerApiDto(
    val loenn: JANEI?,
    val afp: AfpDto? = null,
    val stoenad: List<AndreUtbetalingerYtelserApiDto>?,
) {
    fun tilKontrakt(): AndreUtbetalingerDto = AndreUtbetalingerDto(
        lønn = loenn?.tilKontrakt(),
        afp = afp,
        stønad = stoenad?.map { it.tilKontrakt() },
    )

    companion object {
        fun fraKontrakt(dto: AndreUtbetalingerDto): AndreUtbetalingerApiDto = AndreUtbetalingerApiDto(
            loenn = dto.lønn?.fraKontrakt(),
            afp = dto.afp,
            stoenad = dto.stønad?.map { AndreUtbetalingerYtelserApiDto.fraKontrakt(it) },
        )
    }
}

enum class JANEI {
    JA, NEI;

    fun tilKontrakt() = when (this) {
        JA -> JaNei.Ja
        NEI -> JaNei.Nei
    }
}

fun JaNei.fraKontrakt() = when (this) {
    JaNei.Ja -> JANEI.JA
    JaNei.Nei -> JANEI.NEI
}

enum class AndreUtbetalingerYtelserApiDto {
    OEKONOMISK_SOSIALHJELP,
    OMSORGSSTOENAD,
    INTRODUKSJONSSTOENAD,
    KVALIFISERINGSSTOENAD,
    VERV,
    UTLAND,
    AFP,
    STIPEND,
    LAAN,
    NEI;

    fun tilKontrakt(): AndreUtbetalingerYtelserDto = when (this) {
        OEKONOMISK_SOSIALHJELP -> AndreUtbetalingerYtelserDto.ØKONOMISK_SOSIALHJELP
        OMSORGSSTOENAD -> AndreUtbetalingerYtelserDto.OMSORGSSTØNAD
        INTRODUKSJONSSTOENAD -> AndreUtbetalingerYtelserDto.INTRODUKSJONSSTØNAD
        KVALIFISERINGSSTOENAD -> AndreUtbetalingerYtelserDto.KVALIFISERINGSSTØNAD
        VERV -> AndreUtbetalingerYtelserDto.VERV
        UTLAND -> AndreUtbetalingerYtelserDto.UTLAND
        AFP -> AndreUtbetalingerYtelserDto.AFP
        STIPEND -> AndreUtbetalingerYtelserDto.STIPEND
        LAAN -> AndreUtbetalingerYtelserDto.LÅN
        NEI -> AndreUtbetalingerYtelserDto.NEI
    }

    companion object {
        fun fraKontrakt(dto: AndreUtbetalingerYtelserDto): AndreUtbetalingerYtelserApiDto = when (dto) {
            AndreUtbetalingerYtelserDto.ØKONOMISK_SOSIALHJELP -> OEKONOMISK_SOSIALHJELP
            AndreUtbetalingerYtelserDto.OMSORGSSTØNAD -> OMSORGSSTOENAD
            AndreUtbetalingerYtelserDto.INTRODUKSJONSSTØNAD -> INTRODUKSJONSSTOENAD
            AndreUtbetalingerYtelserDto.KVALIFISERINGSSTØNAD -> KVALIFISERINGSSTOENAD
            AndreUtbetalingerYtelserDto.VERV -> VERV
            AndreUtbetalingerYtelserDto.UTLAND -> UTLAND
            AndreUtbetalingerYtelserDto.AFP -> AFP
            AndreUtbetalingerYtelserDto.STIPEND -> STIPEND
            AndreUtbetalingerYtelserDto.LÅN -> LAAN
            AndreUtbetalingerYtelserDto.NEI -> NEI
        }
    }
}
