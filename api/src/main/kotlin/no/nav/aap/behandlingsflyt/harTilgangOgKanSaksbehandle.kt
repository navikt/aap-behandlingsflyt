package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status

fun harTilgangOgKanSaksbehandle(harTilgang: Boolean, avklaringsbehovene: Avklaringsbehovene): Boolean {
    val erFørAvsluttetKvalitetssikring = avklaringsbehovene.alle()
        .filter { it.definisjon == Definisjon.KVALITETSSIKRING }
        .let { behov ->
            behov.isEmpty() || behov.singleOrNull()?.status() != Status.AVSLUTTET
        }

    val erReturnertFraBeslutter = avklaringsbehovene.alle()
        .filter { it.kreverKvalitetssikring() }
        .any { it.status() == Status.SENDT_TILBAKE_FRA_BESLUTTER }

    return harTilgang && (erFørAvsluttetKvalitetssikring || erReturnertFraBeslutter)
}