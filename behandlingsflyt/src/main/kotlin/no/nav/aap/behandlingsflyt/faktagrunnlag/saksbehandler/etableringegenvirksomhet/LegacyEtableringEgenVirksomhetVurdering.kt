package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant
import java.time.LocalDate


data class LegacyEtableringEgenVirksomhetVurdering(
    val begrunnelse: String,
    val virksomhetNavn: String,
    val orgNr: String? = null,
    val foreliggerFagligVurdering: Boolean,
    val virksomhetErNy: Boolean?,
    val brukerEierVirksomheten: EierVirksomhet?,
    val kanFøreTilSelvforsørget: Boolean?,
    val utviklingsPerioder: List<Periode> = emptyList(),
    val oppstartsPerioder: List<Periode> = emptyList(),
    val vurdertAv: Bruker,
    val opprettet: Instant,
    val vurdertIBehandling: BehandlingId,
    val fom: LocalDate,
    val tom: LocalDate?
)

object EtablerEgenVirksomhetMapper {
    fun fraLegacy(
        legacy: LegacyEtableringEgenVirksomhetVurdering
    ): EtableringEgenVirksomhetVurdering {
        val fase = when {
            legacy.oppstartsPerioder.isNotEmpty() -> EtableringFase.OPPSTART
            legacy.utviklingsPerioder.isNotEmpty() -> EtableringFase.UTVIKLING
            else -> null
        }

        //spm her hvorfor first ?
        val startDato = when (fase) {
            EtableringFase.UTVIKLING -> legacy.utviklingsPerioder.first().fom
            EtableringFase.OPPSTART -> legacy.oppstartsPerioder.first().fom
            null -> legacy.fom
        }

        val sluttDato = when (fase) {
            EtableringFase.UTVIKLING -> legacy.utviklingsPerioder.first().tom
            EtableringFase.OPPSTART -> legacy.oppstartsPerioder.first().tom
            null -> legacy.tom
        }

        val registrert = when (fase) {
            EtableringFase.UTVIKLING -> null
            EtableringFase.OPPSTART -> true
            null -> null
        }

        return EtableringEgenVirksomhetVurdering(
            begrunnelse = legacy.begrunnelse,
            virksomhetNavn = legacy.virksomhetNavn,
            orgNr = legacy.orgNr,
            foreliggerFagligVurdering = legacy.foreliggerFagligVurdering,
            virksomhetErNy = legacy.virksomhetErNy,
            brukerEierVirksomheten = legacy.brukerEierVirksomheten,
            kanFøreTilSelvforsørget = legacy.kanFøreTilSelvforsørget,
            erRegistrertINødvendigeOffentligeRegister = registrert,
            fase = fase,
            vurdertAv = legacy.vurdertAv,
            opprettet = legacy.opprettet,
            vurdertIBehandling = legacy.vurdertIBehandling,
            fom = startDato,
            tom = sluttDato
        )
    }

    fun tilLegacy(
        ny: EtableringEgenVirksomhetVurdering
    ): LegacyEtableringEgenVirksomhetVurdering {
        val fase = ny.fase
        val utviklingsPerioder = if (fase == EtableringFase.UTVIKLING) {
            ny.tom?.let { listOf(Periode(ny.fom, it)) } ?: emptyList()
        } else {
            emptyList()
        }

        val oppstartsPerioder = if (fase == EtableringFase.OPPSTART) {
            ny.tom?.let { listOf(Periode(ny.fom, it)) } ?: emptyList()
        } else {
            emptyList()
        }

        return LegacyEtableringEgenVirksomhetVurdering(
            begrunnelse = ny.begrunnelse,
            virksomhetNavn = ny.virksomhetNavn,
            orgNr = ny.orgNr,
            foreliggerFagligVurdering = ny.foreliggerFagligVurdering,
            virksomhetErNy = ny.virksomhetErNy,
            brukerEierVirksomheten = ny.brukerEierVirksomheten,
            kanFøreTilSelvforsørget = ny.kanFøreTilSelvforsørget,
            utviklingsPerioder = utviklingsPerioder,
            oppstartsPerioder = oppstartsPerioder,
            vurdertAv = ny.vurdertAv,
            opprettet = ny.opprettet,
            vurdertIBehandling = ny.vurdertIBehandling,
            fom = ny.fom,
            tom = ny.tom
        )
    }
}
