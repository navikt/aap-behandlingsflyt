package no.nav.aap.behandlingsflyt.ytelseoppslag

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.UtbetaltePerioder
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Ytelse
import java.time.LocalDate

internal object FakeSykepengerGateway : SykepengerGateway {
    var perioder: List<UtbetaltePerioder> = emptyList()
    var identerBrukt: Set<String> = emptySet()
    var periodeBrukt: Pair<LocalDate, LocalDate>? = null

    fun reset() {
        perioder = emptyList()
        identerBrukt = emptySet()
        periodeBrukt = null
    }

    override fun hentYtelseSykepenger(
        personidentifikatorer: Set<String>,
        fom: LocalDate,
        tom: LocalDate
    ): List<UtbetaltePerioder> {
        identerBrukt = personidentifikatorer
        periodeBrukt = fom to tom
        return perioder
    }
}

internal object FakeForeldrepengerGateway : ForeldrepengerGateway {
    var ytelserPerIdent: Map<String, List<Ytelse>> = emptyMap()
    var identerBrukt: MutableList<String> = mutableListOf()
    var periodeBrukt: Pair<LocalDate, LocalDate>? = null

    fun reset() {
        ytelserPerIdent = emptyMap()
        identerBrukt = mutableListOf()
        periodeBrukt = null
    }

    override fun hentVedtakYtelseForPerson(request: ForeldrepengerRequest): ForeldrepengerResponse {
        identerBrukt.add(request.ident.verdi)
        periodeBrukt = request.periode.fom to request.periode.tom
        return ForeldrepengerResponse(ytelserPerIdent[request.ident.verdi].orEmpty())
    }
}


