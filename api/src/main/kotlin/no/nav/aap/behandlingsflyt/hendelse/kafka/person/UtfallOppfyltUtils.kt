package no.nav.aap.behandlingsflyt.hendelse.kafka.person

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import java.time.Instant
import java.time.ZoneId

class UtfallOppfyltUtils {

    fun allePerioderEtterOpprettetTidspunktHarUtfallIkkeOppfylt(
        opprettetTidspunkt: Instant,
        underveisGrunnlag: UnderveisGrunnlag
    ): Boolean {
        val opprettetDato = opprettetTidspunkt.atZone(ZoneId.of("Europe/Oslo"))?.toLocalDate()

        return underveisGrunnlag.perioder
            .filter { it.periode.fom.isAfter(opprettetDato) }
            .all { it.utfall == Utfall.IKKE_OPPFYLT }
    }
}