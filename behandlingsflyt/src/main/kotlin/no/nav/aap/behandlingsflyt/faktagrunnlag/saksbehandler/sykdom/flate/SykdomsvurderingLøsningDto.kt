package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.SkadekombinasjonRegister
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
    val vedtaksdato: LocalDate? = null,
    val skadeart: String? = null,
    val diagnose: String? = null,
    val skadekombinasjoner: List<SkadekombinasjonRegister>? = null,
    val skadekombinasjonerTekst: String? = null,
) {
    constructor(yrkesskade: Yrkesskade) : this(
        ref = yrkesskade.ref,
        saksnummer = yrkesskade.saksnummer,
        skadedato = yrkesskade.skadedato,
        kilde = yrkesskade.kildesystem,
        vedtaksdato = yrkesskade.vedtaksdato,
        skadeart = yrkesskade.skadeart,
        diagnose = yrkesskade.diagnose,
        skadekombinasjoner = yrkesskade.skadekombinasjoner,
        skadekombinasjonerTekst = yrkesskade.skadekombinasjonerTekst,
    )
}

data class SykdomsvurderingLøsningDto(
    override val begrunnelse: String,

    override val fom: LocalDate,
    override val tom: LocalDate?,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val erArbeidsevnenNedsatt: Boolean?,
    val harSkadeSykdomEllerLyte: Boolean,
    val erSkadeSykdomEllerLyteVesentligdel: Boolean?,
    val erNedsettelseIArbeidsevneAvEnVissVarighet: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean?,
    val erNedsettelseMinstHalvparten: ErNedsettelseMinstHalvpartenValg?,
    val erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean?,
    val erNedsettelseMerEnnYrkesskadegrense: ErNedsettelseMerEnnYrkesskadegrenseValg?,
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
            erNedsettelseMinstHalvparten = utledErNedsettelseMinstHalvparten(),
            erNedsettelseIArbeidsevneAvEnVissVarighet = erNedsettelseIArbeidsevneAvEnVissVarighet,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense,
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
        
        return when (erNedsettelseIArbeidsevneMerEnnHalvparten) {
            true if erNedsettelseIArbeidsevneAvEnVissVarighet == true ->
                ErNedsettelseMinstHalvpartenValg.JA

            true if erNedsettelseIArbeidsevneAvEnVissVarighet == false ->
                ErNedsettelseMinstHalvpartenValg.JA_FORBIGÅENDE_PROBLEMER

            true if erSkadeSykdomEllerLyteVesentligdel == true ->
                ErNedsettelseMinstHalvpartenValg.JA

            false ->
                ErNedsettelseMinstHalvpartenValg.NEI

            else -> null
        }
    }

    private fun utledErNedsettelseMerEnnYrkesskadegrense(): ErNedsettelseMerEnnYrkesskadegrenseValg? {
        if (erNedsettelseMerEnnYrkesskadegrense != null) {
            return erNedsettelseMerEnnYrkesskadegrense
        }

        return when (erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense) {
            true if erNedsettelseIArbeidsevneAvEnVissVarighet == true ->
                ErNedsettelseMerEnnYrkesskadegrenseValg.JA

            true if erNedsettelseIArbeidsevneAvEnVissVarighet == false ->
                ErNedsettelseMerEnnYrkesskadegrenseValg.JA_FORBIGÅENDE_PROBLEMER

            true if erSkadeSykdomEllerLyteVesentligdel == true ->
                ErNedsettelseMerEnnYrkesskadegrenseValg.JA

            false ->
                ErNedsettelseMerEnnYrkesskadegrenseValg.NEI

            else -> null
        }
    }
}
