package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.verdityper.dokument.Kanal
import java.time.LocalDateTime

class MottattDokument(
    val referanse: InnsendingReferanse,
    val sakId: SakId,
    val behandlingId: BehandlingId?,
    val mottattTidspunkt: LocalDateTime,
    val type: InnsendingType,
    val kanal: Kanal,
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
                    return StrukturertDokument(data, brevkategori = strukturertDokument.brevkategori)
                }
                return null
            }

            is StrukturertDokument<*> -> {
                @Suppress("UNCHECKED_CAST")
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
            @Suppress("UNCHECKED_CAST")
            DefaultJsonMapper.toJson((strukturertDokument as StrukturertDokument<Any>).data)
        }
    }
}

