package no.nav.aap.behandlingsflyt.behandling.dialogmelding

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.DokumentInfo
import no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType
import java.time.LocalDateTime

class FellesDialogmeldingDto (
    val innkommendeUtgaaende: InnkommendeUtgaaende,
    val meldingFraNavn: String,
    val opprettetTidspunkt: LocalDateTime,
    val dokumentasjonsType: DokumentasjonType?,
    val tekst: String?,
    val meldingStatus: DialogmeldingLeveringStatus?,
    val journalpostId: String?,
    val dokumentIdListe: MutableList<DokumentInfo>
)