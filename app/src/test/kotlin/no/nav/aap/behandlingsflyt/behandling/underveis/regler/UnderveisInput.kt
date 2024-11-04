package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.Kvote
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.time.Period

val tomUnderveisInput = UnderveisInput(
    rettighetsperiode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1)),
    relevanteVilkår = emptyList(),
    opptrappingPerioder = emptyList(),
    pliktkort = emptyList(),
    innsendingsTidspunkt = mapOf(),
    kvote = Kvote(Period.ZERO),
    aktivitetspliktGrunnlag = AktivitetspliktGrunnlag(emptySet()),
    etAnnetSted = listOf(),
    arbeidsevneGrunnlag = ArbeidsevneGrunnlag(emptyList()),
    meldepliktGrunnlag = MeldepliktGrunnlag(emptyList()),
)