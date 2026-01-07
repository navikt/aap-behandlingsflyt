package no.nav.aap.behandlingsflyt.behandling.inntektsbortfall

import no.nav.aap.behandlingsflyt.behandling.vilkår.inntektsbortfall.InntektsbortfallKanBehandlesAutomatisk
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.InntektsbortfallVurdering

data class InntektsbortfallGrunnlag(
    val inntektsbortfallGrunnlag: InntektsbortfallKanBehandlesAutomatisk,
    val manuellVurdering: InntektsbortfallVurdering?
): Faktagrunnlag