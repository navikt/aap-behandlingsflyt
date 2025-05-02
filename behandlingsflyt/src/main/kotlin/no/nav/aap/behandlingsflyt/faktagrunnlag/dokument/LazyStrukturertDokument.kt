package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.json.DefaultJsonMapper

class LazyStrukturertDokument(
    private val referanse: InnsendingReferanse,
    private val connection: DBConnection
) : StrukturerteData {

    inline fun <reified T : Melding> hentReified(): T {
        return requireNotNull(hent()) { "Fant ingen strukturert data" } as T
    }

    fun hent(): Melding? {
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

        return DefaultJsonMapper.fromJson(strukturerteData, Melding::class.java)
    }

    override fun toString(): String {
        return "LazyStrukturertDokument(connection=$connection, referanse=$referanse)"
    }
}