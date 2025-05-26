package no.nav.aap.behandlingsflyt.repository.sak

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class SakRepositoryImpl(private val connection: DBConnection) : SakRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<SakRepositoryImpl> {
        override fun konstruer(connection: DBConnection): SakRepositoryImpl {
            return SakRepositoryImpl(connection)
        }
    }

    private val personRepository = PersonRepositoryImpl(connection)

    override fun finnEllerOpprett(person: Person, periode: Periode): Sak {
        val relevantesaker = finnSakerFor(person, periode)

        if (relevantesaker.isEmpty()) {
            return opprett(person, periode)
        }

        return relevantesaker.first()
    }

    private fun opprett(person: Person, periode: Periode): Sak {
        val sakId = connection.queryFirst("SELECT nextval('SEQ_SAKSNUMMER') as nextval") {
            setRowMapper { row ->
                row.getLong("nextval")
            }
        }
        val saksnummer = Saksnummer.valueOf(sakId)
        val keys = connection.executeReturnKey(
            "INSERT INTO " +
                    "SAK (saksnummer, person_id, rettighetsperiode, status) " +
                    "VALUES (?, ?, ?::daterange, ?)"
        ) {
            setParams {
                setString(1, saksnummer.toString())
                setLong(2, person.id)
                setPeriode(3, periode)
                setEnumName(4, Status.OPPRETTET)
            }
        }
        log.info("Opprettet sak med ID: $keys. Saksnummer: $saksnummer")
        return Sak(SakId(keys), saksnummer, person, periode)
    }

    override fun oppdaterSakStatus(sakId: SakId, status: Status) {
        val query = """UPDATE sak SET status = ? WHERE ID = ?"""

        return connection.execute(query) {
            setParams {
                setEnumName(1, status)
                setLong(2, sakId.toLong())
            }
            setResultValidator {
                require(it == 1)
            }
        }
    }

    override fun finnAlle(): List<Sak> {
        return connection.queryList(
            """
                SELECT
                 SAK.*,
                 PERSON.REFERANSE AS PERSON_REFERANSE,
                 IDENTER
               FROM SAK
               JOIN PERSON on SAK.person_id = PERSON.id
               JOIN (SELECT 
                    PERSON_IDENT.person_id as person_id,
                    json_agg(json_build_object('identifikator', person_ident.ident, 'aktivIdent', person_ident.primaer)) as IDENTER
                    from PERSON_IDENT
                    GROUP BY PERSON_IDENT.person_id
                ) IDENTER ON PERSON.id = IDENTER.person_id
            """.trimMargin()
        ) {
            setRowMapper { row ->
                Sak(
                    id = SakId(row.getLong("id")),
                    person = Person(
                        id = row.getLong("person_id"),
                        identifikator = row.getUUID("person_referanse"),
                        identer = DefaultJsonMapper.fromJson(row.getString("identer"))
                    ),
                    rettighetsperiode = row.getPeriode("rettighetsperiode"),
                    saksnummer = Saksnummer(row.getString("saksnummer")),
                    status = row.getEnum<Status>("status"),
                    opprettetTidspunkt = row.getLocalDateTime("opprettet_tid")
                )
            }
        }
    }

    override fun finnSakerFor(person: Person): List<Sak> {
        return connection.queryList(
            "SELECT * " +
                    "FROM SAK " +
                    "WHERE person_id = ?"
        ) {
            setParams {
                setLong(1, person.id)
            }
            setRowMapper { row ->
                mapSak(row)
            }
        }
    }

    override fun finnSakerFor(person: Person, periode: Periode): List<Sak> {
        return connection.queryList(
            """SELECT * FROM SAK WHERE person_id = ? AND rettighetsperiode && ?::daterange"""
        ) {
            setParams {
                setLong(1, person.id)
                setPeriode(2, periode)
            }
            setRowMapper { row ->
                mapSak(row)
            }
        }
    }

    override fun hent(sakId: SakId): Sak {
        return connection.queryFirst(
            "SELECT * " +
                    "FROM SAK " +
                    "WHERE id = ?"
        ) {
            setParams {
                setLong(1, sakId.toLong())
            }
            setRowMapper { row ->
                mapSak(row)
            }
        }
    }

    override fun hent(saksnummer: Saksnummer): Sak {
        return connection.queryFirst("SELECT * FROM SAK WHERE saksnummer = ?") {
            setParams {
                setString(1, saksnummer.toString())
            }
            setRowMapper { row ->
                mapSak(row)
            }
        }
    }

    override fun finnSøker(saksnummer: Saksnummer): Person {
        return connection.queryFirst(
            "SELECT person_id " +
                    "FROM SAK " +
                    "WHERE saksnummer = ?"
        ) {
            setParams {
                setString(1, saksnummer.toString())
            }
            setRowMapper { row ->
                personRepository.hent(row.getLong("person_id"))
            }
        }
    }

    override fun finnSøker(sakId: SakId): Person {
        return connection.queryFirst(
            "SELECT person_id " +
                    "FROM SAK " +
                    "WHERE id = ?"
        ) {
            setParams {
                setLong(1, sakId.toLong())
            }
            setRowMapper { row ->
                personRepository.hent(row.getLong("person_id"))
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        // Sak skal ikke slettes ved trekking av søknad
    }

    private fun mapSak(row: Row) = Sak(
        id = SakId(row.getLong("id")),
        person = personRepository.hent(row.getLong("person_id")),
        rettighetsperiode = row.getPeriode("rettighetsperiode"),
        saksnummer = Saksnummer(row.getString("saksnummer")),
        status = row.getEnum("status"),
        opprettetTidspunkt = row.getLocalDateTime("opprettet_tid")
    )

    override fun oppdaterRettighetsperiode(sakId: SakId, periode: Periode) {
        val query = """
            UPDATE SAK SET rettighetsperiode = ?::daterange WHERE id = ?
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setPeriode(1, periode)
                setLong(2, sakId.toLong())
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Denne trengs ikke implementeres
    }
}
