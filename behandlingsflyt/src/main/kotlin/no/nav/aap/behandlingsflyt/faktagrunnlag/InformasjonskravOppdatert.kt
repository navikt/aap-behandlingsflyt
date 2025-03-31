package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.Instant
import java.time.Duration

class InformasjonskravOppdatert(
    val behandlingId: BehandlingId,
    val navn: InformasjonskravNavn,
    val oppdatert: Instant,
) {
    fun tidSidenSistKjøring(now: Instant = Instant.now()) = Duration.between(oppdatert, now)
}

fun InformasjonskravOppdatert?.ikkeKjørtSiste(duration: Duration, now: Instant = Instant.now()): Boolean =
    this == null || tidSidenSistKjøring(now) > duration
