package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon

import HelseinstitusjonVurderingDto
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.HelseinstitusjonGrunnlagResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.InstitusjonsoppholdDto
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class HelseinstitusjonService (
    private val connection: DBConnection,
    private val helseinstitusjonRepository: HelseinstitusjonRepository = HelseinstitusjonRepository(connection),
    private val institusjonRepository: InstitusjonsoppholdService = InstitusjonsoppholdService.konstruer(connection),
    private val behandlingReferanseService: BehandlingReferanseService = BehandlingReferanseService(
        BehandlingRepositoryImpl(connection)
    )
) {
    fun samleHelseinstitusjonGrunnlag(behandlingReferanse: BehandlingReferanse): HelseinstitusjonGrunnlagResponse {
        val behandling: Behandling = behandlingReferanseService.behandling(behandlingReferanse)
        val institusjonsopphold = hentHelseinstitusjonOpphold(behandling.id)
        val vurdering = getHelseinstitusjonVurdering(behandling.id)
        return HelseinstitusjonGrunnlagResponse(institusjonsopphold, vurdering)
    }

    private fun hentHelseinstitusjonOpphold(behandlingId: BehandlingId): List<InstitusjonsoppholdDto> {
        val helseinstitusjonOpphold = institusjonRepository.hentHvisEksisterer(behandlingId)
        return helseinstitusjonOpphold?.opphold?.filter { it.verdi.type == Institusjonstype.HS }
            ?.map { InstitusjonsoppholdDto.institusjonToDto(it) } ?: emptyList()
    }

    private fun getHelseinstitusjonVurdering(behandlingId: BehandlingId) = HelseinstitusjonVurderingDto.toDto(helseinstitusjonRepository.hentAktivHelseinstitusjonVurderingHvisEksisterer(behandlingId))
}