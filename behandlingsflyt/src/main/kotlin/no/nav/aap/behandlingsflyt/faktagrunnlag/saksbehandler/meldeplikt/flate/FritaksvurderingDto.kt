package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import java.time.LocalDate

data class FritaksvurderingDto(
    val harFritak: Boolean,
    val fraDato: LocalDate,
    val begrunnelse: String,
)

data class PeriodisertFritaksvurderingDto(
    override val begrunnelse: String,
    override val fom: LocalDate,
    override val tom: LocalDate?,
    val harFritak: Boolean,
) : LøsningForPeriode