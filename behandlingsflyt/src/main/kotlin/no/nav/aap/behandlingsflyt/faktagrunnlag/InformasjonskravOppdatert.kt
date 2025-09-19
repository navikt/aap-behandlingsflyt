package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class InformasjonskravOppdatert(
    val behandlingId: BehandlingId,
    val navn: InformasjonskravNavn,
    val oppdatert: Instant,
) {
    val datoOppdatert: LocalDate
        get() = oppdatert.atZone(ZoneId.of("Europe/Oslo")).toLocalDate()
}

fun InformasjonskravOppdatert?.ikkeKjørtSisteKalenderdag(): Boolean =
    this == null || datoOppdatert != LocalDate.now()
