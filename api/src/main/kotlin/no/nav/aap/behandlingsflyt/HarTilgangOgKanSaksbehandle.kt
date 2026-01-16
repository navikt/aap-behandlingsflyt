package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status

fun harTilgangOgKanSaksbehandle(harTilgang: Boolean, avklaringsbehovene: Avklaringsbehovene): Boolean {
    val erFørAvsluttetKvalitetssikring = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.KVALITETSSIKRING)
        .let { behov ->
            behov.isEmpty() || behov.singleOrNull()?.status() != Status.AVSLUTTET
        }

    return harTilgang && erFørAvsluttetKvalitetssikring
}