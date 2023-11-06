package no.nav.aap.behandlingsflyt.flyt.vilkår

import no.nav.aap.behandlingsflyt.dbstuff.DBConnection

class LazyFaktaGrunnlag(private val periodeId: Long, private val connection: DBConnection) : Faktagrunnlag {
    override fun hent(): String? {
        return connection.queryFirstOrNull("SELECT faktagrunnlag FROM VILKAR_PERIODE WHERE id = ?") {
            setParams {
                setLong(1, periodeId)
            }
            setRowMapper {
                it.getStringOrNull("faktagrunnlag")
            }
        }
    }

}
