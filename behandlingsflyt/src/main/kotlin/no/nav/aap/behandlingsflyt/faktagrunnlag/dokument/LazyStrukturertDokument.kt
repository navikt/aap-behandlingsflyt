package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Pliktkort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.søknad.Søknad
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.AvvistLegeerklæringId
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingId
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AktivitetskortV0
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper

class LazyStrukturertDokument(
    private val referanse: InnsendingReferanse,
    internal val brevkategori: InnsendingType,
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
            // todo, parse som Melding
            InnsendingType.SØKNAD -> DefaultJsonMapper.fromJson(strukturerteData, Søknad::class.java) as T
            InnsendingType.PLIKTKORT -> DefaultJsonMapper.fromJson(strukturerteData, Pliktkort::class.java) as T
            InnsendingType.AKTIVITETSKORT -> DefaultJsonMapper.fromJson(
                strukturerteData,
                AktivitetskortV0::class.java
            ) as T // TODO, håndter versjonering eget sted?

            // Disse har ikke payload
            InnsendingType.LEGEERKLÆRING_AVVIST -> null
            InnsendingType.LEGEERKLÆRING -> null
            InnsendingType.DIALOGMELDING -> null
        }
    }


}