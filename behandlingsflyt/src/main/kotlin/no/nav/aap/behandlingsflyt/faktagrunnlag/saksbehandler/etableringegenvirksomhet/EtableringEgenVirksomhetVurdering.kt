package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.PeriodisertVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Tid
import java.time.Instant
import java.time.LocalDate

data class EtableringEgenVirksomhetVurdering(
    val begrunnelse: String,
    val virksomhetNavn: String,
    val orgNr: String? = null,
    val foreliggerFagligVurdering: Boolean,
    val virksomhetErNy: Boolean?,
    val brukerEierVirksomheten: EierVirksomhet?,
    val kanFøreTilSelvforsørget: Boolean?,
    override val vurdertAv: Bruker,
    override val opprettet: Instant,
    override val vurdertIBehandling: BehandlingId,
    override val fom: LocalDate,
    override val tom: LocalDate?,
    val utviklingsPerioder: List<Periode> = emptyList(),
    val oppstartsPerioder: List<Periode> = emptyList(),
    val fase: EtableringFase?,
    val erRegistrertINødvendigeOffentligeRegister: Boolean?,
    val jobberBrukerAktivMedVirksomheten: Boolean?,
) : PeriodisertVurdering {
    fun evaluerVirksomhetVurdering(): Boolean {
        return this.fase != null && this.jobberBrukerAktivMedVirksomheten == true && this.virksomhetErNy == true && this.kanFøreTilSelvforsørget == true && this.foreliggerFagligVurdering && this.brukerEierVirksomheten in listOf(
            EierVirksomhet.EIER_MINST_50_PROSENT,
            EierVirksomhet.EIER_MINST_50_PROSENT_MED_FLER
        )
    }
}

fun List<EtableringEgenVirksomhetVurdering>.tilNyttFormat(): List<EtableringEgenVirksomhetVurdering> {
    return this.flatMap { vurdering ->

        if (vurdering.utviklingsPerioder.isEmpty() && vurdering.oppstartsPerioder.isEmpty()) return@flatMap listOf(
            vurdering
        )

        val utviklingsvurderinger = vurdering.utviklingsPerioder.map { utviklingsperiode ->
            vurdering.copy(
                utviklingsPerioder = emptyList(),
                oppstartsPerioder = emptyList(),
                fase = EtableringFase.UTVIKLING,
                fom = utviklingsperiode.fom,
                tom = utviklingsperiode.tom,
                erRegistrertINødvendigeOffentligeRegister = null,
                jobberBrukerAktivMedVirksomheten = true
            )
        }

        val oppstartsvurderinger = vurdering.oppstartsPerioder.map { oppstartsperioder ->
            vurdering.copy(
                utviklingsPerioder = emptyList(),
                oppstartsPerioder = emptyList(),
                fase = EtableringFase.OPPSTART,
                fom = oppstartsperioder.fom,
                tom = oppstartsperioder.tom,
                erRegistrertINødvendigeOffentligeRegister = true,
                jobberBrukerAktivMedVirksomheten = true
            )
        }

        utviklingsvurderinger + oppstartsvurderinger
    }
}

fun EtableringEgenVirksomhetVurdering.tilGammeltFormat(): EtableringEgenVirksomhetVurdering {
    return this.copy(
        oppstartsPerioder = if (this.fase == EtableringFase.OPPSTART) listOf(
            Periode(
                this.fom,
                this.tom ?: Tid.MAKS
            )
        ) else emptyList(),
        utviklingsPerioder = if (this.fase == EtableringFase.UTVIKLING) listOf(
            Periode(
                this.fom,
                this.tom ?: Tid.MAKS
            )
        ) else emptyList(),
    )
}

fun Collection<EtableringEgenVirksomhetVurdering>.tellDager(
    selector: (EtableringEgenVirksomhetVurdering) -> List<Periode>
): Int {
    return this.flatMap(selector).somTidslinje { it }.komprimer().segmenter()
        .sumOf { it.periode.antallHverdager().asInt }
}

enum class EierVirksomhet {
    EIER_MINST_50_PROSENT,
    EIER_MINST_50_PROSENT_MED_FLER,
    NEI
}

enum class EtableringFase(val maksHverdager: Int) {
    UTVIKLING(maksHverdager = 131),
    OPPSTART(maksHverdager = 66);
}

fun List<EtableringEgenVirksomhetVurdering>.erFunksjoneltLik(other: List<EtableringEgenVirksomhetVurdering>): Boolean {
    if (this.size != other.size) return false

    return this.zip(other).all { (a, b) ->
        a.begrunnelse == b.begrunnelse &&
                a.virksomhetNavn == b.virksomhetNavn &&
                a.orgNr == b.orgNr &&
                a.foreliggerFagligVurdering == b.foreliggerFagligVurdering &&
                a.virksomhetErNy == b.virksomhetErNy &&
                a.brukerEierVirksomheten == b.brukerEierVirksomheten &&
                a.kanFøreTilSelvforsørget == b.kanFøreTilSelvforsørget &&
                a.fom == b.fom &&
                a.tom == b.tom &&
                a.fase == b.fase &&
                a.erRegistrertINødvendigeOffentligeRegister == b.erRegistrertINødvendigeOffentligeRegister &&
                a.jobberBrukerAktivMedVirksomheten == b.jobberBrukerAktivMedVirksomheten
    }
}