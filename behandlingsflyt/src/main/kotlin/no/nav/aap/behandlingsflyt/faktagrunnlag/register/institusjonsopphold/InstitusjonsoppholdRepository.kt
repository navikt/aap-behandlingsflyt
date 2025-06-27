package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.Soningsvurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface InstitusjonsoppholdRepository: Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): InstitusjonsoppholdGrunnlag?
    fun hent(behandlingId: BehandlingId): InstitusjonsoppholdGrunnlag
    fun lagreOpphold(behandlingId: BehandlingId, institusjonsopphold: List<Institusjonsopphold>)
    fun lagreSoningsVurdering(behandlingId: BehandlingId, vurdertAv: String, soningsvurderinger: List<Soningsvurdering>)
    fun lagreHelseVurdering(behandlingId: BehandlingId, vurdertAv: String, helseinstitusjonVurderinger: List<HelseinstitusjonVurdering>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}