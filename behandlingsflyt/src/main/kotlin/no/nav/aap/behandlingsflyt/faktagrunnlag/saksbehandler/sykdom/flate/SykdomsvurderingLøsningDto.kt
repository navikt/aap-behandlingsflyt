package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.SkadekombinasjonRegister
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Diagnose
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.Instant
import java.time.LocalDate

data class InnhentetSykdomsOpplysninger(
    val oppgittYrkesskadeISøknad: Boolean? = null,
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
    val harSkadeSykdomEllerLyte: Boolean,
    val harNedsattArbeidsevne: ArbeidsevneNedsattValg? = null,
    val erSkadeSykdomEllerLyteVesentligdel: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean?,
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
            harNedsattArbeidsevne = harNedsattArbeidsevne,
            harSkadeSykdomEllerLyte = harSkadeSykdomEllerLyte,
            erSkadeSykdomEllerLyteVesentligdel = erSkadeSykdomEllerLyteVesentligdel,
            erNedsettelseIArbeidsevneMerEnnHalvparten = erNedsettelseIArbeidsevneMerEnnHalvparten,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense,
            yrkesskadeBegrunnelse = yrkesskadeBegrunnelse,
            diagnose = kodeverk?.let { Diagnose(kodeverk, hoveddiagnose, bidiagnoser) },
            vurdertAv = bruker,
            opprettet = Instant.now(),
            vurdertIBehandling = vurdertIBehandling
        )
    }
}
