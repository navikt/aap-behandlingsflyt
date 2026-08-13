package no.nav.aap.behandlingsflyt.behandling.migrering

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurderingerMetaResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.migrering.MigreringsdatoVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import java.time.LocalDate

data class MigreringsdatoGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurdering: MigreringsdatoVurderingResponse?,
)

data class MigreringsdatoVurderingResponse(
    val migreringsdato: LocalDate,
    val vurderingerMeta: VurderingerMetaResponse,
)

fun MigreringsdatoVurdering.toResponse(vurdertAvService: VurdertAvService) = MigreringsdatoVurderingResponse(
    migreringsdato = migreringsdato,
    vurderingerMeta = vurdertAvService.byggVurderingerMeta(
        definisjon = Definisjon.AVKLAR_MIGRERINGSDATO,
        behandlingId = vurdertIBehandling,
        vurdertAv = vurdertAvService.medNavnOgEnhet(vurdertAv, opprettet),
    ),
)
