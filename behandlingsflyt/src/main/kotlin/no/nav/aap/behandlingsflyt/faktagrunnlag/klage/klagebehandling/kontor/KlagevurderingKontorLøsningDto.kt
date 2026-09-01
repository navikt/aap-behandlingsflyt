package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.KlageInnstilling
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.Klagevurdering
import no.nav.aap.komponenter.verdityper.Bruker

data class KlagevurderingKontorLøsningDto(
    val begrunnelse: String,
    val notat: String?,
    override val innstilling: KlageInnstilling,
    override val vilkårSomOpprettholdes: List<Hjemmel>,
    override val vilkårSomOmgjøres: List<Hjemmel>,
): Klagevurdering {
    init {
       validerHjemler()
    }

    fun tilVurdering(vurdertAv: Bruker) = KlagevurderingKontor(
        begrunnelse = begrunnelse,
        notat = notat,
        innstilling = innstilling,
        vilkårSomOpprettholdes = vilkårSomOpprettholdes,
        vilkårSomOmgjøres = vilkårSomOmgjøres,
        vurdertAv = vurdertAv,
        opprettet = java.time.Instant.now()
    )
}