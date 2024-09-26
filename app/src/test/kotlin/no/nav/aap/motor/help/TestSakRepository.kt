package no.nav.aap.motor.help

import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.sakogbehandling.SakId
import java.util.*

class TestSakRepository(private val connection: DBConnection) {

    fun opprett(person: Person, periode: Periode): SakId {
        val sakId = connection.queryFirst("SELECT nextval('SEQ_SAKSNUMMER') as nextval") {
            setRowMapper { row ->
                row.getLong("nextval")
            }
        }
        val saksnummer = valueOf(sakId)
        val keys = connection.executeReturnKey(
            "INSERT INTO " +
                    "SAK (saksnummer, person_id, rettighetsperiode, status) " +
                    "VALUES (?, ?, ?::daterange, ?)"
        ) {
            setParams {
                setString(1, saksnummer)
                setLong(2, person.id)
                setPeriode(3, periode)
                setEnumName(4, Status.OPPRETTET)
            }
        }
        return SakId(keys)
    }

    /**
     * Gjør saksnummer "human readable"
     */
    private fun valueOf(id: Long): String {
        return (id * 1000).toString(36)
            .uppercase(Locale.getDefault())
            .replace("O", "o")
            .replace("I", "i")
    }
}
