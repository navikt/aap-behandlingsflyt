package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.InntektINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate

interface MedlemskapArbeidInntektRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): MedlemskapArbeidInntektGrunnlag?
    fun hentOppgittUtenlandsOppholdHvisEksisterer(behandlingId: BehandlingId): UtenlandsOppholdData?
    fun hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(sakId: SakId): UtenlandsOppholdData?
    fun lagreArbeidsforholdOgInntektINorge(behandlingId: BehandlingId, relevantDato: LocalDate, arbeidGrunnlag: List<ArbeidINorgeGrunnlag>, inntektGrunnlag: List<InntektINorgeGrunnlag>, medlId: Long?)
    fun lagreOppgittUtenlandsOppplysninger(behandlingId: BehandlingId, journalpostId: JournalpostId, utenlandsOppholdData: UtenlandsOppholdData)
    fun lagreVurderinger(behandlingId: BehandlingId, vurderinger: List<ManuellVurderingForLovvalgMedlemskap>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}