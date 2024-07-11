package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.Institusjonsopphold.InstitusjonsoppholdService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.Institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.SoningsgrunnlagResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.SoningsoppholdDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.SoningsvurderingDto
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class SoningsService(
    private val connection: DBConnection,
    private val soningRepository: SoningRepository = SoningRepository(connection),
    private val institusjonRepository: InstitusjonsoppholdService = InstitusjonsoppholdService.konstruer(connection),
    private val behandlingReferanseService: BehandlingReferanseService = BehandlingReferanseService(connection)
) {

    fun samleSoningsGrunnlag(behandlingsReferanse: BehandlingReferanse): SoningsgrunnlagResponse {
        val behandling: Behandling = behandlingReferanseService.behandling(behandlingsReferanse)
        val soningsvurdering = getSoningsvurderingDto(behandling.id)
        val soningsopphold = getSoningsopphold(behandling.id)

        return SoningsgrunnlagResponse(soningsopphold, soningsvurdering)
    }

    private fun getSoningsopphold(behandlingId: BehandlingId): List<SoningsoppholdDto> {
        val soningsopphold = institusjonRepository.hentHvisEksisterer(behandlingId)
        return soningsopphold?.opphold?.filter { it.verdi.type == Institusjonstype.FO }
            ?.map { SoningsoppholdDto.institusjonToDto(it) } ?: emptyList()
    }

    private fun getSoningsvurderingDto(behandlingId: BehandlingId) = SoningsvurderingDto.toDto(
        soningRepository.hentAktivSoningsvurderingHvisEksisterer(behandlingId)
    )

}