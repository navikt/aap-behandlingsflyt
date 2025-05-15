package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.KlageInnstilling
import java.time.Instant

data class KlagevurderingNay(
    val begrunnelse: String,
    val notat: String?,
    val innstilling: KlageInnstilling,
    val vilkårSomOpprettholdes: List<Hjemmel>,
    val vilkårSomOmgjøres: List<Hjemmel>,
    val vurdertAv: String,
    val opprettet: Instant? = null
)