package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.InntektINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.LovvalgMedlemskapGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import java.time.LocalDate

interface LovvalgMedlemskapRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): LovvalgMedlemskapGrunnlag?
    fun lagreArbeidsforholdOgInntektINorge(behandlingId: BehandlingId, relevantDato: LocalDate, arbeidGrunnlag: List<ArbeidINorgeGrunnlag>, inntektGrunnlag: List<InntektINorgeGrunnlag>, medlId: Long?)
    fun lagreVurderinger(behandlingId: BehandlingId, vurderinger: List<ManuellVurderingForLovvalgMedlemskap>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}