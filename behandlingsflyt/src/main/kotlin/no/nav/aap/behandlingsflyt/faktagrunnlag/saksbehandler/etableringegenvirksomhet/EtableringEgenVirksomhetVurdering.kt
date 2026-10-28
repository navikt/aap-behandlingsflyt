package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.PeriodisertVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Tidslinje
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
    val versjon: EtableringVersjon,
) : PeriodisertVurdering {
    fun evaluerVirksomhetVurdering(): Boolean {
        val grunnvilkårOppfylt = virksomhetErNy == true && kanFøreTilSelvforsørget == true &&
                foreliggerFagligVurdering && brukerEierVirksomheten in listOf(
            EierVirksomhet.EIER_MINST_50_PROSENT,
            EierVirksomhet.EIER_MINST_50_PROSENT_MED_FLER
        )
        return when (versjon) {
            EtableringVersjon.V0 -> grunnvilkårOppfylt
            EtableringVersjon.V1 -> grunnvilkårOppfylt &&
                    jobberBrukerAktivMedVirksomheten == true && when (fase) {
                EtableringFase.UTVIKLING -> true
                EtableringFase.OPPSTART -> erRegistrertINødvendigeOffentligeRegister == true
                null -> false
            }
        }
    }
}

fun List<EtableringEgenVirksomhetVurdering>.tilNyttFormat(): List<EtableringEgenVirksomhetVurdering> {
    return this.flatMap { vurdering ->

        if (vurdering.versjon == EtableringVersjon.V1 ||
            (vurdering.utviklingsPerioder.isEmpty() && vurdering.oppstartsPerioder.isEmpty())
        ) return@flatMap listOf(vurdering)

        val utviklingsvurderinger = vurdering.utviklingsPerioder.map { utviklingsperiode ->
            vurdering.copy(
                utviklingsPerioder = emptyList(),
                oppstartsPerioder = emptyList(),
                fase = EtableringFase.UTVIKLING,
                fom = utviklingsperiode.fom,
                tom = utviklingsperiode.tom,
            )
        }

        val oppstartsvurderinger = vurdering.oppstartsPerioder.map { oppstartsperioder ->
            vurdering.copy(
                utviklingsPerioder = emptyList(),
                oppstartsPerioder = emptyList(),
                fase = EtableringFase.OPPSTART,
                fom = oppstartsperioder.fom,
                tom = oppstartsperioder.tom,
            )
        }

        utviklingsvurderinger + oppstartsvurderinger
    }
}

fun EtableringEgenVirksomhetVurdering.somLegacyVisning(): EtableringEgenVirksomhetVurdering =
    if (fase != null) tilGammeltFormat() else this

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

fun Tidslinje<EtableringEgenVirksomhetVurdering>.antallHverdagerIFase(fase: EtableringFase): Int =
    segmenter()
        .filter { it.verdi.fase == fase }
        .sumOf { it.periode.antallHverdager().asInt }

fun Tidslinje<EtableringEgenVirksomhetVurdering>.perioderForFase(fase: EtableringFase): List<Periode> =
    segmenter()
        .filter { it.verdi.fase == fase }
        .map { it.periode }

fun Collection<EtableringEgenVirksomhetVurdering>.tellDager(
    selector: (EtableringEgenVirksomhetVurdering) -> List<Periode>
): Int {
    return this.map { it.somLegacyVisning() }.flatMap(selector).somTidslinje { it }.komprimer().segmenter()
        .sumOf { it.periode.antallHverdager().asInt }
}

fun List<EtableringEgenVirksomhetVurdering>.brukteDager(): Pair<Int, Int> {
    val gjeldendeVurderinger = gjeldendeFasevurderinger()
    return gjeldendeVurderinger.tellDager { it.utviklingsPerioder } to
        gjeldendeVurderinger.tellDager { it.oppstartsPerioder }
}

enum class EierVirksomhet {
    EIER_MINST_50_PROSENT,
    EIER_MINST_50_PROSENT_MED_FLER,
    NEI
}

enum class EtableringVersjon {
    /** Grunnvilkårene alene avgjør utfallet, også etter oppdeling i faser. */
    V0,
    V1,
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
                a.utviklingsPerioder == b.utviklingsPerioder &&
                a.oppstartsPerioder == b.oppstartsPerioder &&
                a.fom == b.fom &&
                a.tom == b.tom &&
                a.fase == b.fase &&
                a.erRegistrertINødvendigeOffentligeRegister == b.erRegistrertINødvendigeOffentligeRegister &&
                a.jobberBrukerAktivMedVirksomheten == b.jobberBrukerAktivMedVirksomheten &&
                a.versjon == b.versjon
    }
}
