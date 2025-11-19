package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.etannetsted.EtAnnetSted
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravGrunnlag
import no.nav.aap.behandlingsflyt.behandling.underveis.Kvoter
import no.nav.aap.behandlingsflyt.behandling.underveis.tomKvoter
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FastsettMeldeperiodeSteg
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate

fun tomUnderveisInput(
    rettighetsperiode: Periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1)),
    vilkårsresultat: Vilkårsresultat = Vilkårsresultat(
        vilkår = listOf(
            Vilkår(
                type = Vilkårtype.SYKDOMSVILKÅRET, vilkårsperioder = setOf(
                    Vilkårsperiode(
                        periode = rettighetsperiode,
                        utfall = Utfall.OPPFYLT,
                        begrunnelse = null
                    )
                )
            )
        )
    ),
    opptrappingPerioder: List<Periode> = emptyList(),
    meldekort: List<Meldekort> = emptyList(),
    innsendingsTidspunkt: Map<LocalDate, JournalpostId> = emptyMap(),
    kvoter: Kvoter = tomKvoter,
    aktivitetsplikt11_7Grunnlag: Aktivitetsplikt11_7Grunnlag = Aktivitetsplikt11_7Grunnlag(emptyList()),
    etAnnetSted: List<EtAnnetSted> = emptyList(),
    oppholdskravGrunnlag: OppholdskravGrunnlag = OppholdskravGrunnlag(emptyList()),
    arbeidsevneGrunnlag: ArbeidsevneGrunnlag = ArbeidsevneGrunnlag(emptyList()),
    meldepliktGrunnlag: MeldepliktGrunnlag = MeldepliktGrunnlag(emptyList()),
    overstyringMeldepliktGrunnlag: OverstyringMeldepliktGrunnlag = OverstyringMeldepliktGrunnlag(emptyList()),
    meldeperioder: List<Periode> = FastsettMeldeperiodeSteg.utledMeldeperiode(emptyList(), rettighetsperiode),
    vedtaksdatoFørstegangsbehandling: LocalDate? = rettighetsperiode.fom,
): UnderveisInput {
    return UnderveisInput(
        periodeForVurdering = rettighetsperiode,
        vilkårsresultat = vilkårsresultat,
        opptrappingPerioder = opptrappingPerioder,
        meldekort = meldekort,
        innsendingsTidspunkt = innsendingsTidspunkt,
        kvoter = kvoter,
        aktivitetsplikt11_7Grunnlag = aktivitetsplikt11_7Grunnlag,
        oppholdskravGrunnlag = oppholdskravGrunnlag,
        etAnnetSted = etAnnetSted,
        arbeidsevneGrunnlag = arbeidsevneGrunnlag,
        meldepliktGrunnlag = meldepliktGrunnlag,
        overstyringMeldepliktGrunnlag = overstyringMeldepliktGrunnlag,
        meldeperioder = meldeperioder,
        vedtaksdatoFørstegangsbehandling = vedtaksdatoFørstegangsbehandling,
    )
}

val tomUnderveisInput = tomUnderveisInput()
