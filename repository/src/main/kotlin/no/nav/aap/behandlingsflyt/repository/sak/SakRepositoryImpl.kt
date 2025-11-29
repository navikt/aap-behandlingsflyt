package no.nav.aap.behandlingsflyt.repository.sak

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
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

    @WithSpan
    override fun finnEllerOpprett(person: Person, periode: Periode): Sak {
        val relevantesaker = finnSakerFor(person, periode)

        if (relevantesaker.isEmpty()) {
            return opprett(person, Periode(periode.fom, Tid.MAKS))
        }

        return relevantesaker.first()
    }

    @WithSpan
    private fun opprett(person: Person, periode: Periode): Sak {
        val sakId = connection.queryFirst("SELECT nextval('SEQ_SAKSNUMMER') as nextval") {
            setRowMapper { row ->
                row.getLong("nextval")
            }
        }
        val saksnummer = Saksnummer.valueOf(sakId)
        val keys = connection.executeReturnKey(
            """INSERT INTO SAK (saksnummer, person_id, rettighetsperiode, status) VALUES (?, ?, ?::daterange, ?)"""
        ) {
            setParams {
                setString(1, saksnummer.toString())
                setLong(2, person.id.id)
                setPeriode(3, periode)
                setEnumName(4, Status.OPPRETTET)
            }
        }
        log.info("Opprettet sak med ID: $keys. Saksnummer: $saksnummer")
        return Sak(SakId(keys), saksnummer, person, periode)
    }

    @WithSpan
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

    override fun finnAlleSakIder(): List<SakId> {
        return connection.queryList("SELECT id FROM SAK") {
            setRowMapper { row -> SakId(row.getLong("id")) }
        }
    }

    override fun finnSiste(antall: Int): List<Sak> {
        return connection.queryList("SELECT * FROM SAK ORDER BY id DESC LIMIT ?") {
            setParams {
                setInt(1, antall)
            }
            setRowMapper { row -> mapSak(row) }
        }
    }

    override fun finnSakerFor(person: Person): List<Sak> {
        return connection.queryList(
            "SELECT * " +
                    "FROM SAK " +
                    "WHERE person_id = ?"
        ) {
            setParams {
                setLong(1, person.id.id)
            }
            setRowMapper { row ->
                mapSak(row)
            }
        }
    }

    private fun finnSakerFor(person: Person, periode: Periode): List<Sak> {
        return connection.queryList(
            """SELECT * FROM SAK WHERE person_id = ? AND rettighetsperiode && ?::daterange"""
        ) {
            setParams {
                setLong(1, person.id.id)
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

    override fun hentHvisFinnes(saksnummer: Saksnummer): Sak? {
        return connection.queryFirstOrNull("SELECT * FROM SAK WHERE saksnummer = ?") {
            setParams {
                setString(1, saksnummer.toString())
            }
            setRowMapper { row ->
                mapSak(row)
            }
        }
    }

    override fun finnPersonId(sakId: SakId): PersonId {
        return connection.queryFirst(
            """SELECT person_id FROM SAK WHERE id = ?"""
        ) {
            setParams {
                setLong(1, sakId.toLong())
            }
            setRowMapper { row ->
                row.getLong("person_id").let(::PersonId)
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        // Sak skal ikke slettes ved trekking av søknad
    }

    private fun mapSak(row: Row) = Sak(
        id = SakId(row.getLong("id")),
        person = personRepository.hent(row.getLong("person_id").let(::PersonId)),
        rettighetsperiode = row.getPeriode("rettighetsperiode"),
        saksnummer = Saksnummer(row.getString("saksnummer")),
        status = row.getEnum("status"),
        opprettetTidspunkt = row.getLocalDateTime("opprettet_tid")
    )

    @WithSpan
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

    override fun finnSakerMedFritakMeldeplikt(): List<SakId> {
        val sql = """
            select s.id from sak s, behandling b where s.id = b.sak_id and  b.id in (
                select g.behandling_id
                from meldeplikt_fritak_grunnlag g, public.meldeplikt_fritak_vurdering v
                where g.meldeplikt_id = v.meldeplikt_id and g.aktiv = true and g.id in (
                    select id
                    from meldeplikt_fritak_grunnlag
                    where aktiv = true and behandling_id in (
                        select id from behandling where id not in (
                            select forrige_id from behandling where forrige_id is not null
                        )
                    )
                )
            )
        """.trimIndent()

        return connection.queryList(sql) {
            setRowMapper {
                SakId(it.getLong("id"))
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Denne trengs ikke implementeres
    }
}
