package no.nav.aap.behandlingsflyt.dbtest

import no.nav.aap.behandlingsflyt.dbconnect.transaction

object Ident {
    fun hentNesteIdent(): Long {
        return InitTestDatabase.dataSource.transaction { connection ->
            connection.queryFirst("SELECT nextval('IDENT') AS IDENT") {
                setRowMapper { row ->
                    row.getLong("IDENT")
                }
            }
        }
    }
}
