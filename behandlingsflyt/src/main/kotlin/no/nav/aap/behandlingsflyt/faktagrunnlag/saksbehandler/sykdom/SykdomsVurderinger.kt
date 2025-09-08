package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

data class Sykdomsvurdering(
    val id: Long? = null,
    val begrunnelse: String,
    val vurderingenGjelderFra: LocalDate?,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val harSkadeSykdomEllerLyte: Boolean,
    val erSkadeSykdomEllerLyteVesentligdel: Boolean?,
    val erNedsettelseIArbeidsevneAvEnVissVarighet: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean?,
    val yrkesskadeBegrunnelse: String?,
    val erArbeidsevnenNedsatt: Boolean?,
    val kodeverk: String? = null,
    val hoveddiagnose: String? = null,
    val bidiagnoser: List<String>? = emptyList(),
    val opprettet: Instant,
    val vurdertAv: Bruker,
) {
    private fun erAndelNedsattNok(): Boolean {
        return erNedsettelseIArbeidsevneMerEnnHalvparten == true || erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true
    }

    fun erOppfylt(behandlingType: TypeBehandling, kravDato: LocalDate): Boolean {
        return when (behandlingType) {
            TypeBehandling.Førstegangsbehandling -> erOppfyltSettBortIfraVissVarighet() && erNedsettelseIArbeidsevneAvEnVissVarighet == true
            TypeBehandling.Revurdering -> {
                if (vurderingenGjelderFra == null || kravDato == vurderingenGjelderFra) {
                    erOppfyltSettBortIfraVissVarighet() && erNedsettelseIArbeidsevneAvEnVissVarighet == true
                } else {
                    erOppfyltSettBortIfraVissVarighet()
                }
            }

            else -> error("Ugyldig behandlingsType: $behandlingType for vurdering av sykdom.")
        }
    }

    fun erOppfyltSettBortIfraVissVarighet(): Boolean {
        return harSkadeSykdomEllerLyte && erArbeidsevnenNedsatt == true && erSkadeSykdomEllerLyteVesentligdel == true && erAndelNedsattNok()
    }

    fun erKonsistentForSykdom(harYrkesskadeRegistrert: Boolean, typeBehandling: TypeBehandling): Boolean {
        if (!harSkadeSykdomEllerLyte && erSkadeSykdomEllerLyteVesentligdel == true) {
            return false
        }
        if (erArbeidsevnenNedsatt == false && erNedsettelseIArbeidsevneMerEnnHalvparten == true) {
            return false
        }
        if (erNedsettelseIArbeidsevneMerEnnHalvparten != null &&
            !erNedsettelseIArbeidsevneMerEnnHalvparten &&
            harYrkesskadeRegistrert &&
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == null
        ) {
            return false
        }
        if (erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense != null && yrkesskadeBegrunnelse.isNullOrBlank() && typeBehandling == TypeBehandling.Førstegangsbehandling) {
            return false
        }
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Sykdomsvurdering

        if (begrunnelse != other.begrunnelse) return false
        if (vurderingenGjelderFra != other.vurderingenGjelderFra) return false
        if (dokumenterBruktIVurdering != other.dokumenterBruktIVurdering) return false
        if (harSkadeSykdomEllerLyte != other.harSkadeSykdomEllerLyte) return false
        if (erSkadeSykdomEllerLyteVesentligdel != other.erSkadeSykdomEllerLyteVesentligdel) return false
        if (erNedsettelseIArbeidsevneAvEnVissVarighet != other.erNedsettelseIArbeidsevneAvEnVissVarighet) return false
        if (erNedsettelseIArbeidsevneMerEnnHalvparten != other.erNedsettelseIArbeidsevneMerEnnHalvparten) return false
        if (erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense != other.erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense) return false
        if (yrkesskadeBegrunnelse != other.yrkesskadeBegrunnelse) return false
        if (erArbeidsevnenNedsatt != other.erArbeidsevnenNedsatt) return false
        if (kodeverk != other.kodeverk) return false
        if (hoveddiagnose != other.hoveddiagnose) return false
        if (bidiagnoser?.toSet() != other.bidiagnoser?.toSet()) return false
        if (vurdertAv != other.vurdertAv) return false

        return true
    }

    override fun hashCode(): Int {
        var result = begrunnelse.hashCode()
        result = 31 * result + (vurderingenGjelderFra?.hashCode() ?: 0)
        result = 31 * result + dokumenterBruktIVurdering.hashCode()
        result = 31 * result + harSkadeSykdomEllerLyte.hashCode()
        result = 31 * result + (erSkadeSykdomEllerLyteVesentligdel?.hashCode() ?: 0)
        result = 31 * result + (erNedsettelseIArbeidsevneAvEnVissVarighet?.hashCode() ?: 0)
        result = 31 * result + (erNedsettelseIArbeidsevneMerEnnHalvparten?.hashCode() ?: 0)
        result = 31 * result + (erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense?.hashCode() ?: 0)
        result = 31 * result + (yrkesskadeBegrunnelse?.hashCode() ?: 0)
        result = 31 * result + (erArbeidsevnenNedsatt?.hashCode() ?: 0)
        result = 31 * result + (kodeverk?.hashCode() ?: 0)
        result = 31 * result + (hoveddiagnose?.hashCode() ?: 0)
        result = 31 * result + (bidiagnoser?.toSet()?.hashCode() ?: 0)
        result = 31 * result + vurdertAv.hashCode()
        return result
    }
}

data class Yrkesskadevurdering(
    val id: Long? = null,
    val begrunnelse: String,
    val relevanteSaker: List<String>,
    val erÅrsakssammenheng: Boolean,
    val andelAvNedsettelsen: Prosent?,
    val vurdertAv: String,
    val vurdertTidspunkt: LocalDateTime? = null,
)

data class YrkesskadevurderingDto(
    val begrunnelse: String,
    val relevanteSaker: List<String>,
    val andelAvNedsettelsen: Int?,
    val erÅrsakssammenheng: Boolean
)




