package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Pliktkort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.søknad.Søknad
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.verdityper.dokument.JournalpostId
import org.slf4j.LoggerFactory
import java.util.UUID

private val log = LoggerFactory.getLogger(LazyStrukturertDokument::class.java)

class LazyStrukturertDokument(
    private val journalpostId: JournalpostId,
    internal val brevkode: Brevkode,
    private val connection: DBConnection
) : StrukturerteData {

    @Suppress("UNCHECKED_CAST")
    fun <T> hent(): T? {
        val strukturerteData =
            connection.queryFirstOrNull("SELECT strukturert_dokument FROM MOTTATT_DOKUMENT WHERE journalpost = ?") {
                setParams {
                    setString(1, journalpostId.identifikator)
                }
                setRowMapper {
                    it.getStringOrNull("strukturert_dokument")
                }
            }
        if (strukturerteData == null) {
            return null
        }
        return when (brevkode) {
            Brevkode.SØKNAD -> DefaultJsonMapper.fromJson(strukturerteData, Søknad::class.java) as T
            Brevkode.PLIKTKORT -> DefaultJsonMapper.fromJson(strukturerteData, Pliktkort::class.java) as T
            Brevkode.AKTIVITETSKORT -> DefaultJsonMapper.fromJson(strukturerteData, UUID::class.java) as T
            Brevkode.UKJENT -> throw IllegalArgumentException("Ukjent brevkode")
        }
    }


}