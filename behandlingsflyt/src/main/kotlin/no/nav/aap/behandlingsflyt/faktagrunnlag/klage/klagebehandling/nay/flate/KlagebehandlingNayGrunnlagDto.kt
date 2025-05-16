package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlageInnstilling
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagebehandlingNayGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagevurderingNay
import java.time.Instant

data class KlagebehandlingNayGrunnlagDto(
    val vurdering: KlagevurderingNayDto? = null,
)

data class KlagevurderingNayDto(
    val begrunnelse: String,
    val notat: String?,
    val innstilling: KlageInnstilling,
    val vilkårSomOpprettholdes: List<Hjemmel>,
    val vilkårSomOmgjøres: List<Hjemmel>,
    val vurdertAv: String,
    val opprettet: Instant
)

internal fun KlagevurderingNay.tilDto() = 
    KlagevurderingNayDto(
        begrunnelse = begrunnelse,
        notat = notat,
        innstilling = innstilling,
        vilkårSomOpprettholdes = vilkårSomOpprettholdes,
        vilkårSomOmgjøres = vilkårSomOmgjøres,
        vurdertAv = vurdertAv,
        opprettet = opprettet!!
    )

internal fun KlagebehandlingNayGrunnlag.tilDto() =
    KlagebehandlingNayGrunnlagDto(
        vurdering = vurdering.tilDto()
    )