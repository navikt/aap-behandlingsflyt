package no.nav.aap.behandlingsflyt.behandling.behandlerdialog

import java.time.LocalDate
import java.util.UUID

data class MeldingerResponse(
    val meldinger: List<MeldingMedDokumenterDto>,
    val kommendeMeldinger: List<KommendeMeldingDto>
)

data class MeldingMedDokumenterDto(
    val melding: MeldingDto,
    val dokumentIdListe: List<DokumentInfoDto>,
)

data class KommendeMeldingDto(
    val bestillingId: UUID,
    val påminnelseErAvbrutt: Boolean,
    val påminnelseDato: LocalDate
)