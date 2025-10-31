package no.nav.aap.behandlingsflyt.hendelse.kafka.person

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import java.time.Instant
import java.time.ZoneId

class AvslagUtils {

    fun allePerioderEtterOpprettetTidspunktHarAvslagsårsak(
        opprettetTidspunkt: Instant,
        underveisGrunnlag: UnderveisGrunnlag
    ): Boolean {
        val opprettetDato = opprettetTidspunkt.atZone(ZoneId.of("Europe/Oslo"))?.toLocalDate()

        return underveisGrunnlag.perioder
            .filter { it.periode.fom.isAfter(opprettetDato) }
            .all { it.avslagsårsak != null }
    }
}