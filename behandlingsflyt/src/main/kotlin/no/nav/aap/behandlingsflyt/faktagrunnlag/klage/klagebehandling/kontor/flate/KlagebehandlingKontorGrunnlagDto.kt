package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlageInnstilling
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagebehandlingKontorGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagevurderingKontor
import java.time.Instant

data class KlagebehandlingKontorGrunnlagDto(
    val vurdering: KlagevurderingKontorDto? = null,
)

data class KlagevurderingKontorDto(
    val begrunnelse: String,
    val notat: String?,
    val innstilling: KlageInnstilling,
    val vilkårSomOpprettholdes: List<Hjemmel>,
    val vilkårSomOmgjøres: List<Hjemmel>,
    val vurdertAv: String,
    val opprettet: Instant
)

internal fun KlagevurderingKontor.tilDto() = 
    KlagevurderingKontorDto(
        begrunnelse = begrunnelse,
        notat = notat,
        innstilling = innstilling,
        vilkårSomOpprettholdes = vilkårSomOpprettholdes,
        vilkårSomOmgjøres = vilkårSomOmgjøres,
        vurdertAv = vurdertAv,
        opprettet = opprettet!!
    )

internal fun KlagebehandlingKontorGrunnlag.tilDto() =
    KlagebehandlingKontorGrunnlagDto(
        vurdering = vurdering.tilDto()
    )