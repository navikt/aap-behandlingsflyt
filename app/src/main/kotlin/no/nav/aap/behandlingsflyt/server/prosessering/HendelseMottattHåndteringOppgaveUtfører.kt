package no.nav.aap.behandlingsflyt.server.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.UnparsedStrukturertDokument
import no.nav.aap.behandlingsflyt.hendelse.mottak.HåndterMottattDokumentService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.verdityper.sakogbehandling.SakId
import java.time.LocalDateTime

private const val BREVKODE = "brevkode"
private const val MOTTATT_DOKUMENT_REFERANSE = "referanse"
private const val MOTTATT_TIDSPUNKT = "mottattTidspunkt"
private const val PERIODE = "periode"

class HendelseMottattHåndteringOppgaveUtfører(connection: DBConnection) : JobbUtfører {
    private val låsRepository = TaSkriveLåsRepository(connection)
    private val hånderMottattDokumentService = HåndterMottattDokumentService(connection)
    private val mottaDokumentService = MottaDokumentService(MottattDokumentRepository(connection))

    override fun utfør(input: JobbInput) {
        val sakId = SakId(input.sakId())
        val sakSkrivelås = låsRepository.låsSak(sakId)

        val brevkode = Brevkode.valueOf(input.parameter(BREVKODE))
        val payloadAsString = input.payload()
        val mottattTidspunkt = DefaultJsonMapper.fromJson<LocalDateTime>(input.parameter(MOTTATT_TIDSPUNKT))

        val referanse = DefaultJsonMapper.fromJson<MottattDokumentReferanse>(input.parameter(MOTTATT_DOKUMENT_REFERANSE))

        // DO WORK
        mottaDokumentService.mottattDokument(
            referanse = referanse,
            sakId = sakId,
            mottattTidspunkt = mottattTidspunkt,
            brevkode = brevkode,
            strukturertDokument = UnparsedStrukturertDokument(payloadAsString)
        )

        hånderMottattDokumentService.håndterMottatteDokumenter(sakId, brevkode, utledPeriode(input.parameter(PERIODE)))

        låsRepository.verifiserSkrivelås(sakSkrivelås)
    }

    private fun utledPeriode(parameter: String): Periode? {
        if (parameter.isEmpty()) {
            return null
        }

        return DefaultJsonMapper.fromJson(parameter)
    }

    companion object : Jobb {
        fun nyJobb(
            sakId: SakId,
            dokumentReferanse: MottattDokumentReferanse,
            brevkode: Brevkode,
            periode: Periode?,
            payload: Any,
        ) = JobbInput(HendelseMottattHåndteringOppgaveUtfører)
            .apply {
                forSak(sakId.toLong())
                medCallId()
                medParameter(MOTTATT_DOKUMENT_REFERANSE, DefaultJsonMapper.toJson(dokumentReferanse))
                medParameter(BREVKODE, brevkode.name)
                medParameter(PERIODE, if (periode == null) "" else DefaultJsonMapper.toJson(periode))
                medParameter(MOTTATT_TIDSPUNKT, DefaultJsonMapper.toJson(LocalDateTime.now()))
                medPayload(payload?.let { DefaultJsonMapper.toJson(it) })
            }

        override fun konstruer(connection: DBConnection): JobbUtfører {
            return HendelseMottattHåndteringOppgaveUtfører(
                connection
            )
        }

        override fun type(): String {
            return "hendelse.håndterer"
        }

        override fun navn(): String {
            return "Hendelses håndterer"
        }

        override fun beskrivelse(): String {
            return "Håndterer hendelser på en gitt sak. Knytter de nye opplysningene til rett behandling og oppretter behandling hvis det er behov for det."
        }
    }
}