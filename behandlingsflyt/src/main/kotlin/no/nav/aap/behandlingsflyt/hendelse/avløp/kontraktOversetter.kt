package no.nav.aap.behandlingsflyt.hendelse.avløp

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.ÅrsakTilReturKode
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_FUNKSJONALITET
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_KLAGE_IMPLEMENTASJON
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_MASKINELL_AVKLARING
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_FRA_BRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_PÅ_FORHÅNDSVARSEL
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_UTENLANDSK_VIDEREFORING_AVKLARING
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_VURDERING_AV_ROL
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.AvklaringsbehovHendelseDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.EndringDTO
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.ÅrsakTilRetur
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.ÅrsakTilRetur as DomeneÅrsakTilRetur
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.ÅrsakTilReturKode as ÅrsakTilReturKodeKontrakt
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling

fun DomeneÅrsakTilRetur.oversettTilKontrakt(): ÅrsakTilReturKodeKontrakt {
    return when (this.årsak) {
        ÅrsakTilReturKode.MANGELFULL_BEGRUNNELSE -> ÅrsakTilReturKodeKontrakt.MANGELFULL_BEGRUNNELSE
        ÅrsakTilReturKode.MANGLENDE_UTREDNING -> ÅrsakTilReturKodeKontrakt.MANGLENDE_UTREDNING
        ÅrsakTilReturKode.FEIL_LOVANVENDELSE -> ÅrsakTilReturKodeKontrakt.FEIL_LOVANVENDELSE
        ÅrsakTilReturKode.ANNET -> ÅrsakTilReturKodeKontrakt.ANNET
        ÅrsakTilReturKode.SKRIVEFEIL -> ÅrsakTilReturKodeKontrakt.SKRIVEFEIL
        ÅrsakTilReturKode.FOR_DETALJERT -> ÅrsakTilReturKodeKontrakt.FOR_DETALJERT
        ÅrsakTilReturKode.IKKE_INDIVIDUELL_OG_KONKRET -> ÅrsakTilReturKodeKontrakt.IKKE_INDIVIDUELL_OG_KONKRET
    }
}

fun no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.oversettTilKontrakt(): ÅrsakTilSettPåVent {
    return when (this) {
        VENTER_PÅ_OPPLYSNINGER -> ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER
        VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER -> ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER
        VENTER_PÅ_MEDISINSKE_OPPLYSNINGER -> ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER
        VENTER_PÅ_VURDERING_AV_ROL -> ÅrsakTilSettPåVent.VENTER_PÅ_VURDERING_AV_ROL
        VENTER_PÅ_SVAR_FRA_BRUKER -> ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_FRA_BRUKER
        VENTER_PÅ_MASKINELL_AVKLARING -> ÅrsakTilSettPåVent.VENTER_PÅ_MASKINELL_AVKLARING
        VENTER_PÅ_UTENLANDSK_VIDEREFORING_AVKLARING -> ÅrsakTilSettPåVent.VENTER_PÅ_UTENLANDSK_VIDEREFORING_AVKLARING
        VENTER_PÅ_KLAGE_IMPLEMENTASJON -> ÅrsakTilSettPåVent.VENTER_PÅ_KLAGE_IMPLEMENTASJON
        VENTER_PÅ_SVAR_PÅ_FORHÅNDSVARSEL -> ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_PÅ_FORHÅNDSVARSEL
        VENTER_PÅ_FUNKSJONALITET -> ÅrsakTilSettPåVent.VENTER_PÅ_FUNKSJONALITET
    }
}

fun sortererteAvklaringsbehov(
    behandling: Behandling,
    alleAvklaringsbehov: List<no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov>,
): List<AvklaringsbehovHendelseDto> = alleAvklaringsbehov
    .sortedWith(compareBy(behandling.flyt().stegComparator) { it.funnetISteg })
    .map { avklaringsbehov ->
        AvklaringsbehovHendelseDto(
            avklaringsbehovDefinisjon = avklaringsbehov.definisjon,
            status = avklaringsbehov.status(),
            endringer = avklaringsbehov.historikk.map { endring ->
                EndringDTO(
                    status = endring.status,
                    tidsstempel = endring.tidsstempel,
                    endretAv = endring.endretAv,
                    frist = endring.frist,
                    årsakTilSattPåVent = endring.grunn?.oversettTilKontrakt(),
                    begrunnelse = endring.begrunnelse,
                    årsakTilRetur = endring.årsakTilRetur.map {
                        ÅrsakTilRetur(it.oversettTilKontrakt())
                    })
            },
        )
    }