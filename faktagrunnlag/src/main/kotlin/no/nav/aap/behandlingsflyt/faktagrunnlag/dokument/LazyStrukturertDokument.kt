package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.InnsendingId
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Pliktkort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.AvvistLegeerklæringId
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.dokumentinnhenting.Dialogmelding
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.dokumentinnhenting.Legeerklæring
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.søknad.Søknad
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.Brevkategori
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(LazyStrukturertDokument::class.java)

class LazyStrukturertDokument(
    private val referanse: MottattDokumentReferanse,
    internal val brevkategori: Brevkategori,
    private val connection: DBConnection
) : StrukturerteData {

    fun <T> hent(): T? {
        val strukturerteData =
            connection.queryFirstOrNull("SELECT strukturert_dokument FROM MOTTATT_DOKUMENT WHERE referanse_type = ? AND referanse = ?") {
                setParams {
                    setEnumName(1, referanse.type)
                    setString(2, referanse.verdi)
                }
                setRowMapper {
                    it.getStringOrNull("strukturert_dokument")
                }
            }
        if (strukturerteData == null) {
            return null
        }

        @Suppress("UNCHECKED_CAST")
        return when (brevkategori) {
            Brevkategori.SØKNAD -> DefaultJsonMapper.fromJson(strukturerteData, Søknad::class.java) as T
            Brevkategori.PLIKTKORT -> DefaultJsonMapper.fromJson(strukturerteData, Pliktkort::class.java) as T
            Brevkategori.AKTIVITETSKORT -> DefaultJsonMapper.fromJson(strukturerteData, InnsendingId::class.java) as T
            Brevkategori.LEGEERKLÆRING_AVVIST -> DefaultJsonMapper.fromJson(strukturerteData, AvvistLegeerklæringId::class.java) as T
            Brevkategori.LEGEERKLÆRING_MOTTATT -> DefaultJsonMapper.fromJson(strukturerteData, Legeerklæring::class.java) as T
            Brevkategori.DIALOGMELDING -> DefaultJsonMapper.fromJson(strukturerteData, Dialogmelding::class.java) as T
            Brevkategori.UKJENT -> throw IllegalArgumentException("Ukjent brevkode")
        }
    }


}