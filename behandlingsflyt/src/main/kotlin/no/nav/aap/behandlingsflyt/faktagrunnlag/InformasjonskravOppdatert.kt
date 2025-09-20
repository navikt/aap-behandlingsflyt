package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class InformasjonskravOppdatert(
    val behandlingId: BehandlingId,
    val navn: InformasjonskravNavn,
    val oppdatert: Instant,

    /** Rettighetsperioden på tidspunktet informasjonskravet kjørte sist.
     *
     * Siden rettighetsperioden kan endre seg flere ganger i en behandling, så
     * kan man ikke utlede rettighetsperioden på annet vis. Og det er heller
     * ikke garantert at rettighetsperioden her matcher nåverende rettighetsperiode,
     * selv om oppdateringen skjedde i samme behandling.
     */
    val rettighetsperiode: Periode?,
) {
    val datoOppdatert: LocalDate
        get() = oppdatert.atZone(ZoneId.of("Europe/Oslo")).toLocalDate()
}

fun InformasjonskravOppdatert?.ikkeKjørtSisteKalenderdag(): Boolean =
    this == null || datoOppdatert != LocalDate.now()
