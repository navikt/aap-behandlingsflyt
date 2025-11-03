package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.EnhetGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.HistoriskManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMaaned
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.verdityper.dokument.JournalpostId

interface MedlemskapArbeidInntektRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): MedlemskapArbeidInntektGrunnlag?
    fun hentHistoriskeVurderinger(sakId: SakId, behandlingId: BehandlingId): List<HistoriskManuellVurderingForLovvalgMedlemskap>
    fun hentOppgittUtenlandsOppholdHvisEksisterer(behandlingId: BehandlingId): UtenlandsOppholdData?
    fun hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(sakId: SakId): UtenlandsOppholdData?
    fun lagreArbeidsforholdOgInntektINorge(behandlingId: BehandlingId, arbeidGrunnlag: List<ArbeidINorgeGrunnlag>, inntektGrunnlag: List<ArbeidsInntektMaaned>, medlId: Long?, enhetGrunnlag: List<EnhetGrunnlag>)
    fun lagreOppgittUtenlandsOppplysninger(behandlingId: BehandlingId, journalpostId: JournalpostId, utenlandsOppholdData: UtenlandsOppholdData)
    fun lagreVurderinger(behandlingId: BehandlingId, vurderinger: List<ManuellVurderingForLovvalgMedlemskap>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)

    @Deprecated("Fjernes etter migrering")
    fun migrerManuelleVurderingerPeriodisert()
}