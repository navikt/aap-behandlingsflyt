package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.etannetsted.EtAnnetSted
import no.nav.aap.behandlingsflyt.behandling.underveis.Kvote
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Pliktkort
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate

data class UnderveisInput(
    val rettighetsperiode: Periode,
    val relevanteVilkår: List<Vilkår>,
    val opptrappingPerioder: List<Periode>,
    val pliktkort: List<Pliktkort>,
    val innsendingsTidspunkt: Map<LocalDate, JournalpostId>,
    val dødsdato: LocalDate? = null,
    val kvote: Kvote,
    val aktivitetspliktGrunnlag: AktivitetspliktGrunnlag,
    val etAnnetSted: List<EtAnnetSted>,
    val barnetillegg: BarnetilleggGrunnlag
)