package no.nav.aap.behandlingsflyt.faktagrunnlag.register

import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ArbeidsforholdOversikt
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMaaned
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.verdityper.dokument.JournalpostId

interface MedlemskapArbeidInntektRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): MedlemskapArbeidInntektGrunnlag?
    fun hentOppgittUtenlandsOppholdHvisEksisterer(behandlingId: BehandlingId): UtenlandsOppholdData?
    fun lagreArbeidsforholdOgInntektINorge(behandlingId: BehandlingId, arbeidGrunnlag: List<ArbeidsforholdOversikt>, inntektGrunnlag: List<ArbeidsInntektMaaned>, medlId: Long?)
    fun lagreOppgittUtenlandsOppplysninger(behandlingId: BehandlingId, journalpostId: JournalpostId, utenlandsOppholdData: UtenlandsOppholdData)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}