package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
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
    val strukturertDokument: StrukturerteData?
) {

    inline fun <reified T : Melding> strukturerteData(): StrukturertDokument<T>? {
        if (strukturertDokument == null) {
            return null
        }
        when (strukturertDokument) {
            is LazyStrukturertDokument -> {
                val data = strukturertDokument.hentReified<T>()

                return StrukturertDokument(data)
            }

            is StrukturertDokument<*> -> {
                @Suppress("UNCHECKED_CAST")
                return strukturertDokument as StrukturertDokument<*>? as StrukturertDokument<T>?
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
        return when (strukturertDokument) {
            is LazyStrukturertDokument -> {
                val data = strukturertDokument.hent()
                if (data != null) {
                    DefaultJsonMapper.toJson(data)
                } else {
                    null
                }
            }

            is StrukturertDokument<*> -> DefaultJsonMapper.toJson(strukturertDokument.data)
            is UnparsedStrukturertDokument -> strukturertDokument.data
            null -> null
        }
    }
}

