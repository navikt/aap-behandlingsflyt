package no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning

import no.nav.aap.komponenter.gateway.Gateway
import java.time.YearMonth
import no.nav.aap.misc.aordning.InntektskomponentData

interface InntektkomponentenGateway : Gateway {
    fun hentAInntekt(fnr: String, fom: YearMonth, tom: YearMonth): InntektskomponentData
}