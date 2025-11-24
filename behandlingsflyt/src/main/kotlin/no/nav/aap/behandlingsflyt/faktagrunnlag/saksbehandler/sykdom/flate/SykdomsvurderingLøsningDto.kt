package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskade
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

data class SykdomsvurderingLøsningGammelDto(
    val begrunnelse: String,

    /** Hvis null, så gjelder den fra starten. */
    @param:JsonProperty(value = "vurderingenGjelderFra")
    val fom: LocalDate,
    @param:JsonProperty(value = "fom")
    val nyFom: LocalDate?,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val erArbeidsevnenNedsatt: Boolean?,
    val harSkadeSykdomEllerLyte: Boolean,
    val erSkadeSykdomEllerLyteVesentligdel: Boolean?,
    val erNedsettelseIArbeidsevneAvEnVissVarighet: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean?,
    val yrkesskadeBegrunnelse: String?,
    val kodeverk: String? = null,
    val hoveddiagnose: String? = null,
    val bidiagnoser: List<String>? = emptyList(),
) {
    fun tilNyDto(): SykdomsvurderingLøsningDto {
        return SykdomsvurderingLøsningDto(
            begrunnelse = begrunnelse,
            fom = nyFom ?: fom,
            tom = null,
            dokumenterBruktIVurdering = dokumenterBruktIVurdering,
            erArbeidsevnenNedsatt = erArbeidsevnenNedsatt,
            harSkadeSykdomEllerLyte = harSkadeSykdomEllerLyte,
            erSkadeSykdomEllerLyteVesentligdel = erSkadeSykdomEllerLyteVesentligdel,
            erNedsettelseIArbeidsevneMerEnnHalvparten = erNedsettelseIArbeidsevneMerEnnHalvparten,
            erNedsettelseIArbeidsevneAvEnVissVarighet = erNedsettelseIArbeidsevneAvEnVissVarighet,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense,
            yrkesskadeBegrunnelse = yrkesskadeBegrunnelse,
            kodeverk = kodeverk,
            hoveddiagnose = hoveddiagnose,
            bidiagnoser = bidiagnoser,
        )
    }
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
    val yrkesskadeBegrunnelse: String?,
    val kodeverk: String? = null,
    val hoveddiagnose: String? = null,
    val bidiagnoser: List<String>? = emptyList(),
): LøsningForPeriode {
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
            yrkesskadeBegrunnelse = yrkesskadeBegrunnelse,
            kodeverk = kodeverk,
            hoveddiagnose = hoveddiagnose,
            bidiagnoser = bidiagnoser,
            vurdertAv = bruker,
            opprettet = Instant.now(),
            vurdertIBehandling = vurdertIBehandling
        )
    }
}



