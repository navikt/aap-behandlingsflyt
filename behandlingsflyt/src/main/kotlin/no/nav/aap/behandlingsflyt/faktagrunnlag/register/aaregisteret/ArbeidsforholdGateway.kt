package no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.adapter.ArbeidsforholdRequest
import no.nav.aap.lookup.gateway.Gateway

interface ArbeidsforholdGateway : Gateway {
    fun hentAARegisterData(request: ArbeidsforholdRequest): List<ArbeidINorgeGrunnlag>
}