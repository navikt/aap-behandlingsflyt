package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.misc.uføre.UføreSøknad

data class UføreSøknadRequest(val pid: String)
data class UføreSøknadResponse(val soknad: UføreSøknad?)