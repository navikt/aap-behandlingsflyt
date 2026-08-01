package no.nav.aap.behandlingsflyt.steg.underveis.regler

import no.nav.aap.institusjonsopphold.Institusjonsopphold
import no.nav.aap.kvote.Kvoter
import no.nav.aap.misc.Faktagrunnlag
import no.nav.aap.rettighetstype.RettighetstypeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.arbeidsevne.ArbeidsevneGrunnlag
import no.nav.aap.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.meldeplikt.OverstyringMeldepliktGrunnlag
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate

data class UnderveisInput(
    val periodeForVurdering: Periode,
    val vilkårsresultat: Vilkårsresultat,
    val opptrappingPerioder: List<Periode>,
    val meldekort: List<Meldekort>,
    val innsendingsTidspunkt: Map<LocalDate, JournalpostId>,
    val dødsdato: LocalDate? = null,
    val kvoter: Kvoter,
    val institusjonsopphold: List<Institusjonsopphold>,
    val arbeidsevneGrunnlag: ArbeidsevneGrunnlag,
    val meldepliktGrunnlag: MeldepliktGrunnlag,
    val overstyringMeldepliktGrunnlag: OverstyringMeldepliktGrunnlag,
    val meldeperioder: List<Periode>,
    val vedtaksdatoFørstegangsbehandling: LocalDate?,
    val rettighetstypeGrunnlag: RettighetstypeGrunnlag?,
) : Faktagrunnlag