package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.KlageInnstilling
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagevurderingNay
import no.nav.aap.komponenter.httpklient.auth.Bruker

data class KlagevurderingNayLøsningDto(
    val begrunnelse: String,
    val notat: String?,
    val innstilling: KlageInnstilling,
    val vilkårSomOpprettholdes: List<Hjemmel>,
    val vilkårSomOmgjøres: List<Hjemmel>,
) {
    fun tilVurdering(vurdertAv: Bruker) = KlagevurderingNay(
        begrunnelse = begrunnelse,
        notat = notat,
        innstilling = innstilling,
        vilkårSomOpprettholdes = vilkårSomOpprettholdes,
        vilkårSomOmgjøres = vilkårSomOmgjøres,
        vurdertAv = vurdertAv.ident
    )
}