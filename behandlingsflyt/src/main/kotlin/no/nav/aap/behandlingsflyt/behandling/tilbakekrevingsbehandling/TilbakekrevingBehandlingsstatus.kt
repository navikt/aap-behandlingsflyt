package no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon.BehovType
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.AvklaringsbehovHendelseDto
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.tilgang.Rolle

enum class TilbakekrevingBehandlingsstatus {
    OPPRETTET,
    TIL_BEHANDLING,
    TIL_BESLUTTER,
    RETUR_FRA_BESLUTTER,
    AVSLUTTET,
}

fun TilbakekrevingBehandlingsstatus.tilKontrakt(): no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingBehandlingsstatus {
    return when(this){
        TilbakekrevingBehandlingsstatus.AVSLUTTET -> no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingBehandlingsstatus.AVSLUTTET
        TilbakekrevingBehandlingsstatus.OPPRETTET -> no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingBehandlingsstatus.OPPRETTET
        TilbakekrevingBehandlingsstatus.RETUR_FRA_BESLUTTER -> no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingBehandlingsstatus.RETUR_FRA_BESLUTTER
        TilbakekrevingBehandlingsstatus.TIL_BEHANDLING -> no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingBehandlingsstatus.TIL_BEHANDLING
        TilbakekrevingBehandlingsstatus.TIL_BESLUTTER -> no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingBehandlingsstatus.TIL_BESLUTTER
    }
}

fun TilbakekrevingBehandlingsstatus.tilAvklaringsBehov(): List<AvklaringsbehovHendelseDto> {
    return when(this){
  /*      TilbakekrevingBehandlingsstatus.AVSLUTTET -> emptyList()
        TilbakekrevingBehandlingsstatus.OPPRETTET -> AvklaringsbehovKode.`9083`
        TilbakekrevingBehandlingsstatus.RETUR_FRA_BESLUTTER -> listOf(AvklaringsbehovHendelseDto(null,Definisjon.VURDER_TILBAKEKREVING, Status.SENDT_TILBAKE_FRA_BESLUTTER, emptyList()))
        TilbakekrevingBehandlingsstatus.TIL_BEHANDLING -> listOf(AvklaringsbehovHendelseDto(null,Definisjon.VURDER_TILBAKEKREVING, Status.OPPRETTET, emptyList()))
        TilbakekrevingBehandlingsstatus.TIL_BESLUTTER -> listOf(AvklaringsbehovHendelseDto(null,Definisjon.VURDER_TILBAKEKREVING_BESLUTTER, Status.OPPRETTET, emptyList()))
        VURDER_TILBAKEKREVING(
            kode = AvklaringsbehovKode.`9082`,
            type = BehovType.MANUELT_PÅKREVD,
            løsesISteg = StegType.UDEFINERT,
            løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL)
        ),
        VURDER_TILBAKEKREVING_BESLUTTER(
            kode = AvklaringsbehovKode.`9083`,
            type = BehovType.MANUELT_PÅKREVD,
            løsesISteg = StegType.UDEFINERT,
            løsesAv = listOf(Rolle.BESLUTTER)
        )
                VURDER_TILBAKEKREVING(
                kode = AvklaringsbehovKode.`9082`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.UDEFINERT,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL)
            ),
        VURDER_TILBAKEKREVING_BESLUTTER(
            kode = AvklaringsbehovKode.`9083`,
            type = BehovType.MANUELT_PÅKREVD,
            løsesISteg = StegType.UDEFINERT,
            løsesAv = listOf(Rolle.BESLUTTER)
        )(*/
        else -> emptyList()
    }
}