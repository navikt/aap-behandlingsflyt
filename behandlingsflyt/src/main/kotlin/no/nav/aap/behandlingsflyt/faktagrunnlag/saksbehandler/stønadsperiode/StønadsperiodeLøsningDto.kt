package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant
import java.time.LocalDate
import kotlin.Boolean
import kotlin.String

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
            stansOpphør?.type == StansOpphørVurderingTypeDto.STANS -> RelevantKravType.GJENOPPTAK_ETTER_STANS(stansOpphør.årsaker)
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
) {
    init {
        when (type) {
            StansOpphørVurderingTypeDto.STANS -> {
                if (årsaker.isEmpty()) {
                    throw UgyldigForespørselException("Må oppgi hva stans gjennopptas etter")
                }
                val feilTyper = årsaker.filter { it.avslagstype != Avslagstype.STANS }
                if (feilTyper.isNotEmpty()) {
                    throw UgyldigForespørselException("Avslagsårsaker ${feilTyper.joinToString()} er ikke stans-årsaker")
                }
            }
            StansOpphørVurderingTypeDto.OPPHØR -> {
                /* Det gir egentlig god mening å snakke om hva som var årsaken til opphøret.
                 * Men vi har ikke bruk for den informasjonen, så vi krasjer hvis noen prøver
                 * å gi oss den. Men kan vurderes å godta det.
                 */
                if (årsaker.isNotEmpty()) {
                    throw UgyldigForespørselException("Skal ikke oppgi årsak for opphør")
                }
            }
        }
    }
}

enum class StansOpphørVurderingTypeDto {
    STANS, OPPHØR
}