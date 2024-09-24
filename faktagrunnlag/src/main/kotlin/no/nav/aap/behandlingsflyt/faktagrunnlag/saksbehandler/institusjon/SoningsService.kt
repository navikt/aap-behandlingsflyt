package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.InstitusjonsoppholdDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.SoningsgrunnlagResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.SoningsvurderingDto
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class SoningsService(
    connection: DBConnection,
    private val soningRepository: SoningRepository = SoningRepository(connection),
    private val institusjonRepository: InstitusjonsoppholdRepository = InstitusjonsoppholdRepository(connection),
    private val behandlingReferanseService: BehandlingReferanseService = BehandlingReferanseService(
        BehandlingRepositoryImpl(connection)
    )
) {
    fun samleSoningsGrunnlag(behandlingsReferanse: BehandlingReferanse): SoningsgrunnlagResponse {
        val behandling: Behandling = behandlingReferanseService.behandling(behandlingsReferanse)
        val soningsvurdering = getSoningsvurderingDto(behandling.id)
        val soningsopphold = getSoningsopphold(behandling.id)

        return SoningsgrunnlagResponse(soningsopphold, soningsvurdering)
    }

    private fun getSoningsopphold(behandlingId: BehandlingId): List<InstitusjonsoppholdDto> {
        val soningsopphold = institusjonRepository.hentHvisEksisterer(behandlingId)
        return soningsopphold?.opphold?.filter { it.verdi.type == Institusjonstype.FO }
            ?.map { InstitusjonsoppholdDto.institusjonToDto(it) } ?: emptyList()
    }

    private fun getSoningsvurderingDto(behandlingId: BehandlingId) = SoningsvurderingDto.toDto(
        soningRepository.hentAktivSoningsvurderingHvisEksisterer(behandlingId)
    )

}