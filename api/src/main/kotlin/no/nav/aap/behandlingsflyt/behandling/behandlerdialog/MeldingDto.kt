package no.nav.aap.behandlingsflyt.behandling.behandlerdialog

import java.time.LocalDateTime
import java.util.UUID

data class MeldingDto(
    val dialogmeldingId: UUID? = null,
    val innkommendeUtgående: InnkommendeUtgående,
    val meldingFraNavn: String?,
    val opprettetTidspunkt: LocalDateTime,
    val dokumentasjonsType: DokumentasjonType?,
    val tekst: String?,
    val meldingStatus: DialogmeldingLeveringStatus?,
    val journalpostId: String?,
    val påminnelseAvbrutt: Boolean? = null
)