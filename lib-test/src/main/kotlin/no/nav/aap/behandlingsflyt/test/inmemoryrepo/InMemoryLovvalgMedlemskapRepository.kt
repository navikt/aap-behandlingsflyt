package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.lovvalg.LovvalgMedlemskapGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.LovvalgMedlemskapRegisterGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.LovvalgMedlemskapRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryLovvalgMedlemskapRepository : LovvalgMedlemskapRepository {
    private val grunnlag = mutableMapOf<BehandlingId, MutableList<LovvalgMedlemskapGrunnlag>>()
    private val lock = Any()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): LovvalgMedlemskapGrunnlag? =
        synchronized(lock) {
            grunnlag[behandlingId]?.lastOrNull()
        }

    override fun lagreRegisterGrunnlag(
        behandlingId: BehandlingId,
        registerGrunnlag: LovvalgMedlemskapRegisterGrunnlag,
        medlId: Long?
    ) {
        synchronized(lock) {
            val forrige = grunnlag[behandlingId]?.lastOrNull()
            leggTilVersjon(
                behandlingId,
                LovvalgMedlemskapGrunnlag(
                    registerGrunnlag = registerGrunnlag,
                    vurderinger = forrige?.vurderinger ?: emptyList()
                )
            )
        }
    }

    override fun lagreVurderinger(
        behandlingId: BehandlingId,
        vurderinger: List<ManuellVurderingForLovvalgMedlemskap>
    ) {
        synchronized(lock) {
            val forrige = grunnlag[behandlingId]?.lastOrNull()
            leggTilVersjon(
                behandlingId,
                LovvalgMedlemskapGrunnlag(
                    registerGrunnlag = forrige?.registerGrunnlag
                        ?: LovvalgMedlemskapRegisterGrunnlag(null, null, emptyList()),
                    vurderinger = vurderinger
                )
            )
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        require(fraBehandling != tilBehandling)
        synchronized(lock) {
            grunnlag[fraBehandling]?.lastOrNull()?.let { leggTilVersjon(tilBehandling, it) }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(lock) {
            grunnlag.remove(behandlingId)
        }
    }

    private fun leggTilVersjon(behandlingId: BehandlingId, versjon: LovvalgMedlemskapGrunnlag) {
        grunnlag.getOrPut(behandlingId) { mutableListOf() }.add(versjon)
    }
}
