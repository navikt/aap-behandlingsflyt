package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId
import java.time.LocalDateTime

class MottattDokument(
    val journalpostId: JournalpostId,
    val sakId: SakId,
    val behandlingId: BehandlingId?,
    val mottattTidspunkt: LocalDateTime,
    val type: Brevkode,
    val status: Status = Status.MOTTATT,
    private val strukturertDokument: StrukturerteData?
) {

    fun <T> strukturerteData(): StrukturertDokument<out T>? {
        if (strukturertDokument == null) {
            return null
        }
        when (strukturertDokument) {
            is LazyStrukturertDokument -> {
                val data = strukturertDokument.hent<T>()
                if (data != null) {
                    return StrukturertDokument(data, brevkode = strukturertDokument.brevkode)
                }
                return null
            }

            is StrukturertDokument<*> -> {
                return strukturertDokument as StrukturertDokument<T>
            }

            else -> {
                throw IllegalStateException("Ukjent type strukturert dokument")
            }
        }
    }

    fun ustrukturerteData(): String? {
        if (strukturertDokument == null) {
            return null
        }

        return data()
    }

    private fun data(): String? {
        return if (strukturertDokument is LazyStrukturertDokument) {
            val data = strukturertDokument.hent<Any>()
            if (data != null) {
                DefaultJsonMapper.toJson(data)
            } else {
                null
            }
        } else if (strukturertDokument is UnparsedStrukturertDokument) {
            strukturertDokument.data
        } else {
            DefaultJsonMapper.toJson((strukturertDokument as StrukturertDokument<Any>).data)
        }
    }
}