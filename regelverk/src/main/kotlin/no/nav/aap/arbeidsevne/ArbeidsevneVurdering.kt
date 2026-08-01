package no.nav.aap.arbeidsevne

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.misc.PeriodisertVurdering

data class ArbeidsevneVurdering(
    val begrunnelse: String,
    val arbeidsevne: Prosent,
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val vurdertIBehandling: BehandlingId,
    val opprettetTid: LocalDateTime,
    val vurdertAv: Bruker,
) : PeriodisertVurdering {
    fun toArbeidsevneVurderingData(): ArbeidsevneVurderingData {
        return ArbeidsevneVurderingData(
            begrunnelse = begrunnelse,
            arbeidsevne = arbeidsevne,
            opprettetTid = opprettetTid,
            vurdertAv = vurdertAv,
            vurdertIBehandling = vurdertIBehandling
        )
    }

    override val opprettet: Instant = opprettetTid.atZone(ZoneId.of("Europe/Oslo")).toInstant()

    data class ArbeidsevneVurderingData(
        val begrunnelse: String,
        val arbeidsevne: Prosent,
        val opprettetTid: LocalDateTime,
        val vurdertAv: Bruker,
        val vurdertIBehandling: BehandlingId,
    )
}

fun List<ArbeidsevneVurdering>.erFunksjoneltLik(other: List<ArbeidsevneVurdering>): Boolean {
    if (this.size != other.size) return false

    return this.zip(other).all { (a, b) ->
        a.begrunnelse == b.begrunnelse &&
                a.arbeidsevne == b.arbeidsevne &&
                a.fom == b.fom &&
                a.tom == b.tom
    }
}