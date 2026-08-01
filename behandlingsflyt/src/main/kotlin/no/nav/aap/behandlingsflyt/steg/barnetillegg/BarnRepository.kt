package no.nav.aap.behandlingsflyt.steg.barnetillegg

import no.nav.aap.barnetillegg.Barn
import no.nav.aap.barnetillegg.BarnGrunnlag
import no.nav.aap.personopplysninger.Fødselsdato
import no.nav.aap.barnetillegg.OppgitteBarn
import no.nav.aap.barnetillegg.SaksbehandlerOppgitteBarn
import no.nav.aap.barnetillegg.VurdertBarn
import no.nav.aap.barnetillegg.VurderteBarn
import no.nav.aap.misc.Ident
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.komponenter.verdityper.Bruker
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
     * Saksbehandleroppgitte barn er barn som saksbehandler har lagt til manuelt i Kelvin.
     */
    fun lagreSaksbehandlerOppgitteBarn(behandlingId: BehandlingId, saksbehandlerOppgitteBarn: List<SaksbehandlerOppgitteBarn.SaksbehandlerOppgitteBarn>)

    /**
     * Lagre registerbarn. Dette er barn som vi også finner i PDL (enten automatisk, eller oppgitt). Se [BarnInformasjonskrav].
     */
    fun lagreRegisterBarn(behandlingId: BehandlingId, barn: Map<Barn, PersonId?>)

    /**
     * Lagre vurderinger på barn. Gjøres i løseren, [no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklarBarnetilleggLøser].
     */
    fun lagreVurderinger(behandlingId: BehandlingId, vurdertAv: Bruker, vurderteBarn: List<VurdertBarn>)
    fun finnFødselsdatoForRegisterBarn(ident: Ident): Fødselsdato?
    fun hentBehandlingIdForSakSomFårBarnetilleggForRegisterBarn(ident: Ident): List<BehandlingId>
    fun hentBehandlingIdForSakSomFårBarnetilleggForSaksbehandlerOppgitteBarn(ident: Ident): List<BehandlingId>
    fun hentBehandlingIdForSakSomFårBarnetilleggForSøknadsBarn(ident: Ident): List<BehandlingId>
    fun finnSaksbehandlerOppgitteBarn(ident: Ident):  SaksbehandlerOppgitteBarn.SaksbehandlerOppgitteBarn?
    fun finnSøknadsBarn(ident: Ident):  OppgitteBarn.OppgittBarn?
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    fun tilbakestillGrunnlag(behandlingId: BehandlingId, forrigeBehandlingId: BehandlingId?)
}
