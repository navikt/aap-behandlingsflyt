package no.nav.aap.behandlingsflyt.steg.medlemskap

import no.nav.aap.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.lovvalg.EnhetGrunnlag
import no.nav.aap.behandlingsflyt.steg.lovvalg.MedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.steg.lovvalg.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.misc.aordning.ArbeidsInntektMåned
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.verdityper.dokument.JournalpostId

interface MedlemskapArbeidInntektRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): MedlemskapArbeidInntektGrunnlag?
    fun hentOppgittUtenlandsOppholdHvisEksisterer(behandlingId: BehandlingId): UtenlandsOppholdData?
    fun hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(sakId: SakId): UtenlandsOppholdData?
    fun lagreArbeidsforholdOgInntektINorge(behandlingId: BehandlingId, arbeidGrunnlag: List<ArbeidINorgeGrunnlag>, inntektGrunnlag: List<ArbeidsInntektMåned>, medlId: Long?, enhetGrunnlag: List<EnhetGrunnlag>)
    fun lagreOppgittUtenlandsOppplysninger(behandlingId: BehandlingId, journalpostId: JournalpostId, utenlandsOppholdData: UtenlandsOppholdData)
    fun lagreVurderinger(behandlingId: BehandlingId, vurderinger: List<ManuellVurderingForLovvalgMedlemskap>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}