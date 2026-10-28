package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.tilNyttFormat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.utenPerioder
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid

internal data class Migreringssteg(
    val vurderingId: Long,
    val opprinneligVurdering: EtableringEgenVirksomhetVurdering,
    val oppdaterteVurderinger: List<EtableringEgenVirksomhetVurdering>,
    val skalErstattes: Boolean,
) {
    val vurderingerEtterMigrering: List<EtableringEgenVirksomhetVurdering>
        get() = if (skalErstattes) oppdaterteVurderinger else listOf(opprinneligVurdering)
}

internal fun EtableringEgenVirksomhetVurdering.tilMigrerteVurderinger():
    List<EtableringEgenVirksomhetVurdering> {
    // Gammelt format har alltid perioder, nytt format har aldri perioder
    if (utviklingsPerioder.isEmpty() && oppstartsPerioder.isEmpty()) {
        return emptyList()
    }
    return listOf(this).tilNyttFormat()
}

internal fun lagMigreringsplan(
    vurderinger: List<Pair<Long, EtableringEgenVirksomhetVurdering>>
): List<Migreringssteg> {
    val prioriterteVurderinger = vurderinger
        .groupBy { (_, vurdering) -> vurdering.vurdertIBehandling }
        .values
        .sortedBy { gruppe -> gruppe.first().second.opprettet }
        .flatMap { gruppe -> gruppe.sortedBy { (_, vurdering) -> vurdering.fom } }

    return prioriterteVurderinger.mapIndexed { index, (vurderingId, vurdering) ->
        val oppdaterteVurderinger = vurdering.tilMigrerteVurderinger()
        if (oppdaterteVurderinger.isEmpty()) {
            Migreringssteg(
                vurderingId = vurderingId,
                opprinneligVurdering = vurdering,
                oppdaterteVurderinger = emptyList(),
                skalErstattes = false,
            )
        } else {
            // En nyere vurdering overstyrer hele gyldighetsperioden, også der den ikke har en fase.
            val overstyrendePerioder = prioriterteVurderinger
                .drop(index + 1)
                .map { (_, overstyrendeVurdering) ->
                    Periode(overstyrendeVurdering.fom, overstyrendeVurdering.tom ?: Tid.MAKS)
                }
            Migreringssteg(
                vurderingId = vurderingId,
                opprinneligVurdering = vurdering,
                oppdaterteVurderinger = oppdaterteVurderinger.flatMap { oppdatertVurdering ->
                    oppdatertVurdering.utenPerioder(overstyrendePerioder)
                },
                skalErstattes = true,
            )
        }
    }
}

private fun Periode.uten(overstyrendePeriode: Periode): List<Periode> {
    return this.minus(overstyrendePeriode).toList()
}

internal fun DBConnection.hentVurderingerGruppeId(vurderingId: Long): Long =
    queryFirstOrNull(
        """
            SELECT vurderinger_id
            FROM etablering_egen_virksomhet_vurdering
            WHERE id = ?
        """.trimIndent()
    ) {
        setParams { setLong(1, vurderingId) }
        setRowMapper { it.getLong("vurderinger_id") }
    } ?: error("Fant ikke vurderingsgruppe for vurdering $vurderingId")
