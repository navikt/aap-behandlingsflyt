package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.lookup.repository.Repository

interface BarnRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): BarnGrunnlag?
    fun hentVurderteBarnHvisEksisterer(behandlingId: BehandlingId): VurderteBarn?
    fun hent(behandlingId: BehandlingId): BarnGrunnlag

    /**
     * Oppgitte barn er barn som er oppgitt i søknaden. De lagres når behandlingen opprettes. Se [no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.søknad.SøknadInformasjonskrav].
     */
    fun lagreOppgitteBarn(behandlingId: BehandlingId, oppgitteBarn: OppgitteBarn)

    /**
     * Lagre registerbarn. Dette er barn som vi også finner i PDL (enten automatisk, eller oppgitt). Se [no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnInformasjonskrav].
     */
    fun lagreRegisterBarn(behandlingId: BehandlingId, barn: Map<Barn, PersonId?>)

    /**
     * Lagre vurderinger på barn. Gjøres i løseren, [no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarBarnetilleggLøser].
     */
    fun lagreVurderinger(behandlingId: BehandlingId, vurdertAv: String, vurderteBarn: List<VurdertBarn>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}
