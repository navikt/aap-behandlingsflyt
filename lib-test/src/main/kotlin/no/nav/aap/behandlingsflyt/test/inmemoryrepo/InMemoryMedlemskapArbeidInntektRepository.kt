package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.EnhetGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgMedlemskapVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMåned
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.verdityper.dokument.JournalpostId

object InMemoryMedlemskapArbeidInntektRepository : MedlemskapArbeidInntektRepository {
    private val mutex = Any()
    private val grunnlag = HashMap<BehandlingId, MedlemskapArbeidInntektGrunnlag>()
    private val oppgittUtenlandsOpphold = HashMap<BehandlingId, UtenlandsOppholdData>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): MedlemskapArbeidInntektGrunnlag? =
        synchronized(mutex) { grunnlag[behandlingId] }

    override fun hentOppgittUtenlandsOppholdHvisEksisterer(behandlingId: BehandlingId): UtenlandsOppholdData? =
        synchronized(mutex) { oppgittUtenlandsOpphold[behandlingId] }

    // Ingen kobling mellom sak og behandling i denne implementasjonen
    override fun hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(sakId: SakId): UtenlandsOppholdData? = null

    // Inntekter og MEDL-data mappes ikke; kun arbeidsforhold beholdes
    override fun lagreArbeidsforholdOgInntektINorge(
        behandlingId: BehandlingId,
        arbeidGrunnlag: List<ArbeidINorgeGrunnlag>,
        inntektGrunnlag: List<ArbeidsInntektMåned>,
        medlId: Long?,
        enhetGrunnlag: List<EnhetGrunnlag>
    ) = synchronized(mutex) {
        val eksisterende = grunnlag[behandlingId] ?: tomtGrunnlag()
        grunnlag[behandlingId] = eksisterende.copy(arbeiderINorgeGrunnlag = arbeidGrunnlag)
    }

    override fun lagreOppgittUtenlandsOppplysninger(
        behandlingId: BehandlingId,
        journalpostId: JournalpostId,
        utenlandsOppholdData: UtenlandsOppholdData
    ) {
        synchronized(mutex) { oppgittUtenlandsOpphold[behandlingId] = utenlandsOppholdData }
    }

    override fun lagreVurderinger(behandlingId: BehandlingId, vurderinger: List<LovvalgMedlemskapVurdering>) =
        synchronized(mutex) {
            val eksisterende = grunnlag[behandlingId] ?: tomtGrunnlag()
            grunnlag[behandlingId] = eksisterende.copy(vurderinger = vurderinger)
        }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        synchronized(mutex) {
            grunnlag[fraBehandling]?.let { grunnlag[tilBehandling] = it }
            oppgittUtenlandsOpphold[fraBehandling]?.let { oppgittUtenlandsOpphold[tilBehandling] = it }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(mutex) {
            grunnlag.remove(behandlingId)
            oppgittUtenlandsOpphold.remove(behandlingId)
        }
    }

    // Kun for tester: setter hele grunnlaget direkte, inkludert data lagre-metodene ikke støtter (f.eks. MEDL)
    fun settGrunnlag(behandlingId: BehandlingId, medlemskapArbeidInntektGrunnlag: MedlemskapArbeidInntektGrunnlag) {
        synchronized(mutex) { grunnlag[behandlingId] = medlemskapArbeidInntektGrunnlag }
    }

    private fun tomtGrunnlag() = MedlemskapArbeidInntektGrunnlag(
        medlemskapGrunnlag = null,
        inntekterINorgeGrunnlag = emptyList(),
        arbeiderINorgeGrunnlag = emptyList(),
    )
}
