package no.nav.aap.behandlingsflyt.steg.institusjon

import no.nav.aap.institusjonsopphold.HelseinstitusjonVurdering
import no.nav.aap.institusjonsopphold.Soningsvurdering
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.misc.institusjonsopphold.Institusjonsopphold
import no.nav.aap.misc.institusjonsopphold.InstitusjonsoppholdGrunnlag

interface InstitusjonsoppholdRepository: Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): InstitusjonsoppholdGrunnlag?
    fun hent(behandlingId: BehandlingId): InstitusjonsoppholdGrunnlag
    fun lagreOpphold(behandlingId: BehandlingId, institusjonsopphold: List<Institusjonsopphold>)
    fun lagreSoningsVurdering(behandlingId: BehandlingId, vurdertAv: Bruker, soningsvurderinger: List<Soningsvurdering>)
    fun lagreHelseVurdering(behandlingId: BehandlingId, helseinstitusjonVurderinger: List<HelseinstitusjonVurdering>)
    fun hentVurderingerGruppertPerOpphold(behandlingId: BehandlingId): Map<Periode, List<HelseinstitusjonVurdering>>
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}