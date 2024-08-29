package no.nav.aap.behandlingsflyt.dbtestdata

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.verdityper.sakogbehandling.Ident

fun ident(): Ident {
    val ident = Ident(hentNesteIdent().toString())
    return ident
}

private fun hentNesteIdent(): Long {
    return InitTestDatabase.dataSource.transaction { connection ->
        connection.queryFirst("SELECT nextval('TESTIDENT') AS IDENT") {
            setRowMapper { row ->
                row.getLong("IDENT")
            }
        }
    }
}
