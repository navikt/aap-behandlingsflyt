package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn

import no.nav.aap.verdityper.sakogbehandling.Ident

class VurdertBarnDto(val ident: String, val vurderinger: List<VurderingAvForeldreAnsvar>) {
    fun toVurdertBarn(): VurdertBarn {
        return VurdertBarn(Ident(ident), vurderinger)
    }
}