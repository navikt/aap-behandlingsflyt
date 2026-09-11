package no.nav.aap.behandlingsflyt.behandling.inntektsbortfall

import no.nav.aap.behandlingsflyt.behandling.vilkår.inntektsbortfall.InntektsbortfallKanBehandlesAutomatisk
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.InntektsbortfallVurdering
import no.nav.aap.komponenter.type.Periode

data class InntektsbortfallGrunnlag(
    val inntektsbortfallKanBehandlesAutomatisk: InntektsbortfallKanBehandlesAutomatisk?,
    val manuellVurdering: InntektsbortfallVurdering?,
    val rettighetsPeriode: Periode,
) : Faktagrunnlag