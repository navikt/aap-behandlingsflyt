package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate

import no.nav.aap.meldeplikt.OverstyringMeldepliktVurderingPeriode

data class MeldepliktOverstyringLøsningDto(
    val perioder: List<OverstyringMeldepliktVurderingPeriode>
)
