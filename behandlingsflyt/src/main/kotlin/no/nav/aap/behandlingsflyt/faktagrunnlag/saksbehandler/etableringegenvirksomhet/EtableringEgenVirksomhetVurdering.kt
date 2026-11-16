package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.PeriodisertVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
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
    val fase: EtableringFase?,
    val erRegistrertINødvendigeOffentligeRegister: Boolean?,
    val jobberBrukerAktivMedVirksomheten: Boolean?,
    val versjon: EtableringVersjon,
) : PeriodisertVurdering {
    fun erGrunnvilkårOppfylt(): Boolean =
        virksomhetErNy == true && kanFøreTilSelvforsørget == true &&
                foreliggerFagligVurdering && brukerEierVirksomheten in listOf(
            EierVirksomhet.EIER_MINST_50_PROSENT,
            EierVirksomhet.EIER_MINST_50_PROSENT_MED_FLER
        )

    fun evaluerVirksomhetVurdering(): Boolean {
        val grunnvilkårOppfylt = erGrunnvilkårOppfylt()
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

fun Tidslinje<EtableringEgenVirksomhetVurdering>.antallHverdagerIFase(fase: EtableringFase): Int =
    segmenter()
        .filter { it.verdi.fase == fase }
        .sumOf { it.periode.antallHverdager().asInt }

fun Tidslinje<EtableringEgenVirksomhetVurdering>.perioderForFase(fase: EtableringFase): List<Periode> =
    segmenter()
        .filter { it.verdi.fase == fase }
        .map { it.periode }

enum class EierVirksomhet {
    EIER_MINST_50_PROSENT,
    EIER_MINST_50_PROSENT_MED_FLER,
    NEI
}

enum class EtableringVersjon {
    /** Grunnvilkårene alene avgjør utfallet, også etter oppdeling i faser. */
    V0,
    V1
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
                a.jobberBrukerAktivMedVirksomheten == b.jobberBrukerAktivMedVirksomheten &&
                a.versjon == b.versjon
    }
}

/*
 - slette utv.perioder og utv.periode-tabeller (og oppstart)
 - slett                 v.egen_virksomhet_utvikling_perioder_id,
                v.egen_virksomhet_oppstart_perioder_id

 */