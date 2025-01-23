package no.nav.aap.behandlingsflyt.faktagrunnlag.register

import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ArbeidsforholdOversikt
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMaaned
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface MedlemskapLovvalgRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): MedlemskapLovvalgGrunnlag?
    fun lagreArbeidsforholdOgInntektINorge(behandlingId: BehandlingId, arbeidGrunnlag: List<ArbeidsforholdOversikt>, inntektGrunnlag: List<ArbeidsInntektMaaned>, medlId: Long?)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}