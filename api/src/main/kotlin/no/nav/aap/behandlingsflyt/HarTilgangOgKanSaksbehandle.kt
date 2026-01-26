package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status

fun harTilgangOgKanSaksbehandle(harTilgang: Boolean, avklaringsbehovene: Avklaringsbehovene): Boolean {
    val harÅpneAvklaringsbehov = avklaringsbehovene.alle()
        .filter { it.kreverKvalitetssikring() || it.definisjon == Definisjon.REFUSJON_KRAV }
        .any { it.status() == Status.OPPRETTET }

    val erReturnertFraKvalitetssikrer = avklaringsbehovene.alle()
        .filter { it.kreverKvalitetssikring() }
        .any { it.status() == Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER }

    val erReturnertFraBeslutter = avklaringsbehovene.alle()
        .filter { it.kreverKvalitetssikring() }
        .any { it.status() == Status.SENDT_TILBAKE_FRA_BESLUTTER }

    return harTilgang && (harÅpneAvklaringsbehov || erReturnertFraKvalitetssikrer || erReturnertFraBeslutter)
}