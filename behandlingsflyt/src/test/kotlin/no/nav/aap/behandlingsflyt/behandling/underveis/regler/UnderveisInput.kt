package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.etannetsted.EtAnnetSted
import no.nav.aap.behandlingsflyt.behandling.underveis.Kvoter
import no.nav.aap.behandlingsflyt.behandling.underveis.tomKvoter
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRimeligGrunnGrunnlag
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
    innsendingsTidspunkt: Map<LocalDate, JournalpostId> = mapOf(),
    kvoter: Kvoter = tomKvoter,
    aktivitetspliktGrunnlag: AktivitetspliktGrunnlag = AktivitetspliktGrunnlag(emptySet()),
    etAnnetSted: List<EtAnnetSted> = listOf(),
    arbeidsevneGrunnlag: ArbeidsevneGrunnlag = ArbeidsevneGrunnlag(emptyList()),
    meldepliktGrunnlag: MeldepliktGrunnlag = MeldepliktGrunnlag(emptyList()),
    meldepliktRimeligGrunnGrunnlag: MeldepliktRimeligGrunnGrunnlag = MeldepliktRimeligGrunnGrunnlag(emptyList()),
    meldeperioder: List<Periode> = FastsettMeldeperiodeSteg.utledMeldeperiode(listOf(), rettighetsperiode),
    vedtaksdatoFørstegangsbehandling: LocalDate? = rettighetsperiode.fom,
): UnderveisInput {
    return UnderveisInput(
        rettighetsperiode = rettighetsperiode,
        vilkårsresultat = vilkårsresultat,
        opptrappingPerioder = opptrappingPerioder,
        meldekort = meldekort,
        innsendingsTidspunkt = innsendingsTidspunkt,
        kvoter = kvoter,
        aktivitetspliktGrunnlag = aktivitetspliktGrunnlag,
        etAnnetSted = etAnnetSted,
        arbeidsevneGrunnlag = arbeidsevneGrunnlag,
        meldepliktGrunnlag = meldepliktGrunnlag,
        meldepliktRimeligGrunnGrunnlag = meldepliktRimeligGrunnGrunnlag,
        meldeperioder = meldeperioder,
        vedtaksdatoFørstegangsbehandling = vedtaksdatoFørstegangsbehandling,
    )
}

val tomUnderveisInput = tomUnderveisInput()
