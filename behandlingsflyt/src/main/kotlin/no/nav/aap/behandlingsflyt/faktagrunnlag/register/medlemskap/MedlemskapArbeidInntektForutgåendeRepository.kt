package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.HistoriskManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMaaned
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository

interface MedlemskapArbeidInntektForutgåendeRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): ForutgåendeMedlemskapArbeidInntektGrunnlag?
    fun hentHistoriskeVurderinger(sakId: SakId, behandlingId: BehandlingId): List<HistoriskManuellVurderingForForutgåendeMedlemskap>
    fun lagreArbeidsforholdOgInntektINorge(behandlingId: BehandlingId, arbeidGrunnlag: List<ArbeidINorgeGrunnlag>, inntektGrunnlag: List<ArbeidsInntektMaaned>, medlId: Long?)
    fun lagreManuellVurdering(behandlingId: BehandlingId, manuellVurdering: ManuellVurderingForForutgåendeMedlemskap)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}