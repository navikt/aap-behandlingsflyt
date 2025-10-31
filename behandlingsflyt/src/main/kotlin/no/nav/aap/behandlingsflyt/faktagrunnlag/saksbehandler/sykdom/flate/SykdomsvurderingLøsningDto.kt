package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
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
    val begrunnelse: String,

    /** Hvis null, så gjelder den fra starten. */
    val vurderingenGjelderFra: LocalDate?,
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
    fun toSykdomsvurdering(
        bruker: Bruker,
        vurdertIBehandling: BehandlingId,
        defaultGjelderFra: LocalDate
    ): Sykdomsvurdering {
        return Sykdomsvurdering(
            begrunnelse = begrunnelse,
            vurderingenGjelderFra = vurderingenGjelderFra ?: defaultGjelderFra,
            vurderingenGjelderTil = null, // TODO: Støtt ny periodisert løsning
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

    fun valider() {
        val errors = mutableListOf<String>()
        if (!harSkadeSykdomEllerLyte) {
            if (!kodeverk.isNullOrBlank()) {
                errors.add("Kodeverk kan ikke være satt med mindre det er skade, sykdom eller lyte.")
            }
            if (!hoveddiagnose.isNullOrBlank()) {
                errors.add("Hoveddiagnose kan ikke være satt med mindre det er skade, sykdom eller lyte.")
            }
            if (!bidiagnoser.isNullOrEmpty()) {
                errors.add("Bidiagnoser kan ikke være satt med mindre det er skade, sykdom eller lyte.")
            }
            if (erArbeidsevnenNedsatt != null) {
                errors.add("Nedsatt arbeidsevne kan ikke være satt med mindre det er skade, sykdom eller lyte.")
            }
        }
        if (erArbeidsevnenNedsatt != null && !erArbeidsevnenNedsatt) {
            if (erSkadeSykdomEllerLyteVesentligdel != null) {
                errors.add("Skade, sykdom eller lyte vesentlig del kan ikke være satt hvis arbeidsevnen ikke er nedsatt.")
            }
            if (erNedsettelseIArbeidsevneAvEnVissVarighet != null) {
                errors.add("Nedsettelse i arbeidsevne av en viss varighet kan ikke være satt hvis arbeidsevnen ikke er nedsatt.")
            }
            if (erNedsettelseIArbeidsevneMerEnnHalvparten != null) {
                errors.add("Nedsettelse i arbeidsevne mer enn halvparten kan ikke være satt hvis arbeidsevnen ikke er nedsatt.")
            }
            if (erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense != null) {
                errors.add("Nedsettelse i arbeidsevne mer enn yrkesskadegrense kan ikke være satt hvis arbeidsevnen ikke er nedsatt.")
            }
        }
        if (errors.isNotEmpty()) {
            throw UgyldigForespørselException("Ugyldig sykdomsvurdering: \n${errors.joinToString("\n - ", " - ")}")
        }
    }
}



