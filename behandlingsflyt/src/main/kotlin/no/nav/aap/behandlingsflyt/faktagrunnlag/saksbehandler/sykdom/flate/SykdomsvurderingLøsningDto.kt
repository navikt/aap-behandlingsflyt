package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Diagnose
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ErNedsettelseMerEnnYrkesskadegrenseValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ErNedsettelseMinstHalvpartenValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.Instant
import java.time.LocalDate

data class InnhentetSykdomsOpplysninger(
    val oppgittYrkesskadeISøknad: Boolean,
    val innhentedeYrkesskader: List<RegistrertYrkesskade>,
)

data class RegistrertYrkesskade(
    val ref: String,
    val saksnummer: Int?,
    val skadedato: LocalDate?,
    val kilde: String,
) {
    constructor(yrkesskade: Yrkesskade) : this(
        yrkesskade.ref,
        yrkesskade.saksnummer,
        yrkesskade.skadedato,
        yrkesskade.kildesystem
    )
}

data class SykdomsvurderingLøsningDto(
    override val begrunnelse: String,

    /** Hvis null, så gjelder den fra starten. */
    override val fom: LocalDate,
    override val tom: LocalDate?,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val erArbeidsevnenNedsatt: Boolean?,
    val harSkadeSykdomEllerLyte: Boolean,
    val erSkadeSykdomEllerLyteVesentligdel: Boolean?,
    val erNedsettelseIArbeidsevneAvEnVissVarighet: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean?,
    val erNedsettelseMinstHalvparten: ErNedsettelseMinstHalvpartenValg? = null,
    val erNedsettelseMerEnnYrkesskadegrense: ErNedsettelseMerEnnYrkesskadegrenseValg? = null,
    val yrkesskadeBegrunnelse: String?,
    val kodeverk: String? = null,
    val hoveddiagnose: String? = null,
    val bidiagnoser: List<String>? = emptyList(),
) : LøsningForPeriode {
    fun toSykdomsvurdering(
        bruker: Bruker,
        vurdertIBehandling: BehandlingId,
    ): Sykdomsvurdering {
        return Sykdomsvurdering(
            begrunnelse = begrunnelse,
            vurderingenGjelderFra = fom,
            vurderingenGjelderTil = tom,
            dokumenterBruktIVurdering = dokumenterBruktIVurdering,
            erArbeidsevnenNedsatt = erArbeidsevnenNedsatt,
            harSkadeSykdomEllerLyte = harSkadeSykdomEllerLyte,
            erSkadeSykdomEllerLyteVesentligdel = erSkadeSykdomEllerLyteVesentligdel,
            erNedsettelseIArbeidsevneMerEnnHalvparten = erNedsettelseIArbeidsevneMerEnnHalvparten,
            erNedsettelseIArbeidsevneAvEnVissVarighet = erNedsettelseIArbeidsevneAvEnVissVarighet,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense,
            erNedsettelseMinstHalvparten = utledErNedsettelseMinstHalvparten(),
            erNedsettelseMerEnnYrkesskadegrense = utledErNedsettelseMerEnnYrkesskadegrense(),
            yrkesskadeBegrunnelse = yrkesskadeBegrunnelse,
            diagnose = kodeverk?.let { Diagnose(kodeverk, hoveddiagnose, bidiagnoser) },
            vurdertAv = bruker,
            opprettet = Instant.now(),
            vurdertIBehandling = vurdertIBehandling
        )
    }
    
    private fun utledErNedsettelseMinstHalvparten(): ErNedsettelseMinstHalvpartenValg? {
        if (erNedsettelseMinstHalvparten != null) {
            return erNedsettelseMinstHalvparten
        }

        return when {
            erNedsettelseIArbeidsevneMerEnnHalvparten == true && erNedsettelseIArbeidsevneAvEnVissVarighet == true -> 
                ErNedsettelseMinstHalvpartenValg.JA
            erNedsettelseIArbeidsevneMerEnnHalvparten == true && erNedsettelseIArbeidsevneAvEnVissVarighet == false -> 
                ErNedsettelseMinstHalvpartenValg.JA_FORBIGÅENDE_PROBLEMER
            erNedsettelseIArbeidsevneMerEnnHalvparten == false -> 
                ErNedsettelseMinstHalvpartenValg.NEI
            else -> null
        }
    }
    
    private fun utledErNedsettelseMerEnnYrkesskadegrense(): ErNedsettelseMerEnnYrkesskadegrenseValg? {
        if (erNedsettelseMerEnnYrkesskadegrense != null) {
            return erNedsettelseMerEnnYrkesskadegrense
        }

        return when {
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true && erNedsettelseIArbeidsevneAvEnVissVarighet == true -> 
                ErNedsettelseMerEnnYrkesskadegrenseValg.JA
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true && erNedsettelseIArbeidsevneAvEnVissVarighet == false -> 
                ErNedsettelseMerEnnYrkesskadegrenseValg.JA_FORBIGÅENDE_PROBLEMER
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == false -> 
                ErNedsettelseMerEnnYrkesskadegrenseValg.NEI
            else -> null
        }
    }
}
