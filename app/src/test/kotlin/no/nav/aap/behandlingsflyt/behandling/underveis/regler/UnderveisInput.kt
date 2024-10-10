package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.Kvote
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetspliktGrunnlag
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
    bruddAktivitetspliktGrunnlag = BruddAktivitetspliktGrunnlag(emptySet()),
    etAnnetSted = listOf(),
    barnetillegg = BarnetilleggGrunnlag(0, listOf()),
)