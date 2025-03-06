package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.tomKvoter
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

val tomUnderveisInput = UnderveisInput(
    rettighetsperiode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1)),
    vilkårsresultat = Vilkårsresultat(
        vilkår = listOf(
            Vilkår(
                type = Vilkårtype.SYKDOMSVILKÅRET, vilkårsperioder = setOf(
                    Vilkårsperiode(
                        periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1)),
                        utfall = Utfall.OPPFYLT,
                        begrunnelse = null
                    )
                )
            )
        )
    ),
    opptrappingPerioder = emptyList(),
    meldekort = emptyList(),
    innsendingsTidspunkt = mapOf(),
    kvoter = tomKvoter,
    aktivitetspliktGrunnlag = AktivitetspliktGrunnlag(emptySet()),
    etAnnetSted = listOf(),
    arbeidsevneGrunnlag = ArbeidsevneGrunnlag(emptyList()),
    meldepliktGrunnlag = MeldepliktGrunnlag(emptyList()),
)