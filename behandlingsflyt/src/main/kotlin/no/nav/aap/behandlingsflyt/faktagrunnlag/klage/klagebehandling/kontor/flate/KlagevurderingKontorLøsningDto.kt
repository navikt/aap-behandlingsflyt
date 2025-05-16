package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.KlageInnstilling
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagevurderingKontor
import no.nav.aap.komponenter.httpklient.auth.Bruker

data class KlagevurderingKontorLøsningDto(
    val begrunnelse: String,
    val notat: String?,
    val innstilling: KlageInnstilling,
    val vilkårSomOpprettholdes: List<Hjemmel>,
    val vilkårSomOmgjøres: List<Hjemmel>,
) {
    init {
        require(
            when (innstilling) {
                KlageInnstilling.OPPRETTHOLD -> vilkårSomOpprettholdes.isNotEmpty() && vilkårSomOmgjøres.isEmpty()
                KlageInnstilling.OMGJØR -> vilkårSomOmgjøres.isNotEmpty() && vilkårSomOpprettholdes.isEmpty()
                KlageInnstilling.DELVIS_OMGJØR -> vilkårSomOmgjøres.isNotEmpty()
                        && vilkårSomOpprettholdes.isNotEmpty()
                        && vilkårSomOmgjøres != vilkårSomOpprettholdes
            }
        ) {
            "Ugyldig kombinasjon av innstilling og vilkår"
        }
    }

    fun tilVurdering(vurdertAv: Bruker) = KlagevurderingKontor(
        begrunnelse = begrunnelse,
        notat = notat,
        innstilling = innstilling,
        vilkårSomOpprettholdes = vilkårSomOpprettholdes,
        vilkårSomOmgjøres = vilkårSomOmgjøres,
        vurdertAv = vurdertAv.ident
    )
}