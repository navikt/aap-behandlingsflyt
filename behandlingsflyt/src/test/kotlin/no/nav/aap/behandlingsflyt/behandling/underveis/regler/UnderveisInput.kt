package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.institusjonsopphold.Institusjonsopphold
import no.nav.aap.kvote.Kvoter
import no.nav.aap.behandlingsflyt.behandling.underveis.tomKvoter
import no.nav.aap.behandlingsflyt.steg.meldeperiode.MeldeperiodeUtleder
import no.nav.aap.rettighetstype.RettighetstypeGrunnlag
import no.nav.aap.vilkårsresultat.Utfall
import no.nav.aap.vilkårsresultat.Vilkår
import no.nav.aap.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårsresultat
import no.nav.aap.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.arbeidsevne.ArbeidsevneGrunnlag
import no.nav.aap.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.meldeplikt.OverstyringMeldepliktGrunnlag
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate
import no.nav.aap.behandlingsflyt.steg.underveis.regler.UnderveisInput

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
    institusjonsopphold: List<Institusjonsopphold> = emptyList(),
    arbeidsevneGrunnlag: ArbeidsevneGrunnlag = ArbeidsevneGrunnlag(emptyList()),
    meldepliktGrunnlag: MeldepliktGrunnlag = MeldepliktGrunnlag(emptyList()),
    overstyringMeldepliktGrunnlag: OverstyringMeldepliktGrunnlag = OverstyringMeldepliktGrunnlag(emptyList()),
    meldeperioder: List<Periode> = MeldeperiodeUtleder.utledMeldeperiode(null, rettighetsperiode),
    vedtaksdatoFørstegangsbehandling: LocalDate? = rettighetsperiode.fom,
    rettighetstypeGrunnlag: RettighetstypeGrunnlag? = null,
): UnderveisInput {
    return UnderveisInput(
        periodeForVurdering = rettighetsperiode,
        vilkårsresultat = vilkårsresultat,
        opptrappingPerioder = opptrappingPerioder,
        meldekort = meldekort,
        innsendingsTidspunkt = innsendingsTidspunkt,
        kvoter = kvoter,
        institusjonsopphold = institusjonsopphold,
        arbeidsevneGrunnlag = arbeidsevneGrunnlag,
        meldepliktGrunnlag = meldepliktGrunnlag,
        overstyringMeldepliktGrunnlag = overstyringMeldepliktGrunnlag,
        meldeperioder = meldeperioder,
        vedtaksdatoFørstegangsbehandling = vedtaksdatoFørstegangsbehandling,
        rettighetstypeGrunnlag = rettighetstypeGrunnlag,
    )
}

val tomUnderveisInput = tomUnderveisInput()
