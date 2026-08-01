package no.nav.aap.behandlingsflyt.steg.stønadsperiode

import no.nav.aap.vilkårsresultat.Avslagsårsak
import no.nav.aap.krav.Kravreferanse
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant
import java.time.LocalDate
import kotlin.Boolean
import kotlin.String
import no.nav.aap.stønadsperiode.RelevantKravType
import no.nav.aap.stønadsperiode.StønadsperiodeVurdering

// TODO: Håndter avslag § 12
data class StønadsperiodeLøsningDto(
    val referanse: Kravreferanse,
    val begrunnelse: String,
    val harHattOrdinærSiste52Uker: Boolean,
    val harGjenværendeKvote: Boolean,
    val stansOpphør: StansEllerOpphørDto?,
    val startDato: LocalDate,
) {
    val relevantKravType: RelevantKravType
        get() = when {
            !harHattOrdinærSiste52Uker && !harGjenværendeKvote -> RelevantKravType.NY_STØNADSPERIODE
            stansOpphør?.type == StansOpphørVurderingTypeDto.STANS -> RelevantKravType.GJENOPPTAK_ETTER_STANS
            stansOpphør?.type == StansOpphørVurderingTypeDto.OPPHØR -> RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR
            else -> throw IllegalStateException("Klarte ikke utlede kravtype")
        }

    fun tilVurdering(
        bruker: Bruker,
        vurdertIBehandling: BehandlingId,
        opprettet: Instant = Instant.now()
    ): StønadsperiodeVurdering {
        if (stansOpphør == null && (harGjenværendeKvote || harHattOrdinærSiste52Uker)) {
            throw UgyldigForespørselException("Stans/opphør-årsak er påkrevd ved gjenopptak/gjeninntreden")
        }

        // TODO: Lagre ned stansopphør
        return StønadsperiodeVurdering(
            referanse = referanse,
            begrunnelse = begrunnelse,
            harGjenværendeKvote = harGjenværendeKvote,
            harHattOrdinærSiste52Uker = harHattOrdinærSiste52Uker,
            startDato = startDato,
            relevantKravType = relevantKravType,
            opprettet = opprettet,
            vurdertAv = bruker,
            vurdertIBehandling = vurdertIBehandling
        )
    }
}

data class StansEllerOpphørDto(
    val type: StansOpphørVurderingTypeDto, // TODO: Ønsker vi heller å utlede stans/opphør utifra årsak?
    val årsaker: List<Avslagsårsak>,
)

enum class StansOpphørVurderingTypeDto {
    STANS, OPPHØR
}