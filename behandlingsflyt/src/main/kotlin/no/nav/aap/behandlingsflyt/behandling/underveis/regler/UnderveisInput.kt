package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.etannetsted.EtAnnetSted
import no.nav.aap.behandlingsflyt.behandling.underveis.Kvoter
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktGrunnlag
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate

data class UnderveisInput(
    val rettighetsperiode: Periode,
    val vilkårsresultat: Vilkårsresultat,
    val opptrappingPerioder: List<Periode>,
    val meldekort: List<Meldekort>,
    val innsendingsTidspunkt: Map<LocalDate, JournalpostId>,
    val dødsdato: LocalDate? = null,
    val kvoter: Kvoter,
    val aktivitetspliktGrunnlag: AktivitetspliktGrunnlag,
    val etAnnetSted: List<EtAnnetSted>,
    val arbeidsevneGrunnlag: ArbeidsevneGrunnlag,
    val meldepliktGrunnlag: MeldepliktGrunnlag,
    val overstyringMeldepliktGrunnlag: OverstyringMeldepliktGrunnlag,
    val meldeperioder: List<Periode>,
    val vedtaksdatoFørstegangsbehandling: LocalDate?,
) : Faktagrunnlag