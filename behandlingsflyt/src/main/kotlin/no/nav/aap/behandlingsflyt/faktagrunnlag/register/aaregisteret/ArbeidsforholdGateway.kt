package no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret

import no.nav.aap.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.komponenter.gateway.Gateway

interface ArbeidsforholdGateway : Gateway {
    fun hentAARegisterData(request: ArbeidsforholdRequest): List<ArbeidINorgeGrunnlag>
}