package no.nav.aap.behandlingsflyt.steg.medlemskap

import no.nav.aap.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.lovvalg.EnhetGrunnlag
import no.nav.aap.lovvalg.ForutgåendeMedlemskapArbeidInntektGrunnlag
import no.nav.aap.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.misc.aordning.ArbeidsInntektMåned
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface MedlemskapArbeidInntektForutgåendeRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): ForutgåendeMedlemskapArbeidInntektGrunnlag?
    fun lagreArbeidsforholdOgInntektINorge(behandlingId: BehandlingId, arbeidGrunnlag: List<ArbeidINorgeGrunnlag>, inntektGrunnlag: List<ArbeidsInntektMåned>, medlId: Long?, enhetGrunnlag: List<EnhetGrunnlag>)
    fun lagreVurderinger(behandlingId: BehandlingId, vurderinger: List<ManuellVurderingForForutgåendeMedlemskap>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}