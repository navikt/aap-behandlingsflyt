package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.lovvalgmedlemskap

import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.OppgittUtenlandsopplysningerRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.verdityper.dokument.JournalpostId
import org.slf4j.LoggerFactory

class OppgittUtenlandsopplysningerRepositoryImpl(private val connection: DBConnection) :
    OppgittUtenlandsopplysningerRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<OppgittUtenlandsopplysningerRepositoryImpl> {
        override fun konstruer(connection: DBConnection): OppgittUtenlandsopplysningerRepositoryImpl {
            return OppgittUtenlandsopplysningerRepositoryImpl(connection)
        }
    }

    override fun hentOppgittUtenlandsOppholdHvisEksisterer(behandlingId: BehandlingId): UtenlandsOppholdData? {
        val query = """
            SELECT * FROM OPPGITT_UTENLANDSOPPHOLD_GRUNNLAG WHERE behandling_id = ? and aktiv = true
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                hentOppgittUtenlandsOpphold(it.getLong("oppgitt_utenlandsopphold_id"))
            }
        }
    }

    override fun hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(sakId: SakId): UtenlandsOppholdData? {
        val query = """
            SELECT *
            FROM OPPGITT_UTENLANDSOPPHOLD_GRUNNLAG grunnlag
            JOIN BEHANDLING ON grunnlag.BEHANDLING_ID = behandling.ID
            LEFT JOIN AVBRYT_REVURDERING_GRUNNLAG AR ON AR.BEHANDLING_ID = BEHANDLING.ID
            WHERE grunnlag.AKTIV AND behandling.SAK_ID = ? AND AR.BEHANDLING_ID IS NULL
            ORDER BY behandling.OPPRETTET_TID DESC
            LIMIT 1
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, sakId.id)
            }
            setRowMapper {
                hentOppgittUtenlandsOpphold(it.getLong("oppgitt_utenlandsopphold_id"))
            }
        }
    }

    override fun lagreOppgittUtenlandsOppplysninger(
        behandlingId: BehandlingId,
        journalpostId: JournalpostId,
        utenlandsOppholdData: UtenlandsOppholdData
    ) {
        val eksisterendeGrunnlag = hentOppgittUtenlandsOppholdHvisEksisterer(behandlingId)
        if (eksisterendeGrunnlag != null) {
            deaktiverUtenlandsOppholdGrunnlag(behandlingId)
        }

        val oppgittUtenlandsOppholdQuery = """
            INSERT INTO OPPGITT_UTENLANDSOPPHOLD (BODD_I_NORGE_SISTE_FEM_AAR, ARBEIDET_I_NORGE_SISTE_FEM_AAR, ARBEIDET_UTENFOR_NORGE_FOR_SYKDOM, I_TILLEGG_ARBEID_UTENFOR_NORGE) VALUES (?, ?, ?, ?)
        """.trimIndent()

        val oppgittUtenlandsOppholdId = connection.executeReturnKey(oppgittUtenlandsOppholdQuery) {
            setParams {
                setBoolean(1, utenlandsOppholdData.harBoddINorgeSiste5År)
                setBoolean(2, utenlandsOppholdData.harArbeidetINorgeSiste5År)
                setBoolean(3, utenlandsOppholdData.arbeidetUtenforNorgeFørSykdom)
                setBoolean(4, utenlandsOppholdData.iTilleggArbeidUtenforNorge)
            }
        }

        val oppgittPeriodeQuery = """
            INSERT INTO UTENLANDS_PERIODE (LAND, TIL_DATO, FRA_DATO, I_ARBEID, UTENLANDS_ID, OPPGITT_UTENLANDSOPPHOLD_ID) VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()

        if (!utenlandsOppholdData.utenlandsOpphold.isNullOrEmpty()) {
            for (opphold in utenlandsOppholdData.utenlandsOpphold!!) {
                connection.execute(oppgittPeriodeQuery) {
                    setParams {
                        setString(1, opphold.land)
                        setLocalDate(2, opphold.tilDato)
                        setLocalDate(3, opphold.fraDato)
                        setBoolean(4, opphold.iArbeid)
                        setString(5, opphold.utenlandsId)
                        setLong(6, oppgittUtenlandsOppholdId)
                    }
                }
            }
        }

        val oppgittUtenlandsOppholdGrunnlagQuery = """
            INSERT INTO OPPGITT_UTENLANDSOPPHOLD_GRUNNLAG (BEHANDLING_ID, JOURNALPOST_ID, OPPGITT_UTENLANDSOPPHOLD_ID) VALUES (?, ?, ?)
        """.trimIndent()

        connection.execute(oppgittUtenlandsOppholdGrunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setString(2, journalpostId.identifikator)
                setLong(3, oppgittUtenlandsOppholdId)
            }
        }
    }

    private fun hentOppgittUtenlandsOpphold(oppgittUtenlandsOppholdId: Long): UtenlandsOppholdData {
        val utenlandsPeriode = connection.queryList(
            """SELECT * FROM UTENLANDS_PERIODE WHERE OPPGITT_UTENLANDSOPPHOLD_ID = ?""".trimIndent()
        ) {
            setParams {
                setLong(1, oppgittUtenlandsOppholdId)
            }
            setRowMapper {
                UtenlandsPeriode(
                    land = it.getStringOrNull("land"),
                    tilDato = it.getLocalDateOrNull("til_dato"),
                    fraDato = it.getLocalDateOrNull("fra_dato"),
                    iArbeid = it.getBoolean("i_arbeid"),
                    utenlandsId = it.getStringOrNull("utenlands_id")
                )
            }
        }

        return connection.queryFirst(
            """SELECT * FROM OPPGITT_UTENLANDSOPPHOLD WHERE ID = ?""".trimIndent()
        ) {
            setParams {
                setLong(1, oppgittUtenlandsOppholdId)
            }
            setRowMapper {
                UtenlandsOppholdData(
                    harBoddINorgeSiste5År = it.getBoolean("bodd_i_norge_siste_fem_aar"),
                    harArbeidetINorgeSiste5År = it.getBoolean("arbeidet_i_norge_siste_fem_aar"),
                    arbeidetUtenforNorgeFørSykdom = it.getBoolean("arbeidet_utenfor_norge_for_sykdom"),
                    iTilleggArbeidUtenforNorge = it.getBoolean("i_tillegg_arbeid_utenfor_norge"),
                    utenlandsOpphold = utenlandsPeriode
                )
            }
        }
    }

    private fun deaktiverUtenlandsOppholdGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE OPPGITT_UTENLANDSOPPHOLD_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Opplysningene hentes fra siste relevante behandling i saken og skal ikke kopieres.
    }

    override fun slett(behandlingId: BehandlingId) {
        val utenlandsOppholdIds = connection.queryList(
            """
                SELECT oppgitt_utenlandsopphold_id
                FROM OPPGITT_UTENLANDSOPPHOLD_GRUNNLAG
                WHERE behandling_id = ?
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper { it.getLong("oppgitt_utenlandsopphold_id") }
        }

        val deletedRows = connection.executeReturnUpdated(
            """
                delete from UTENLANDS_PERIODE where oppgitt_utenlandsopphold_id = ANY(?::bigint[]);
                delete from OPPGITT_UTENLANDSOPPHOLD_GRUNNLAG where behandling_id = ?;
                delete from OPPGITT_UTENLANDSOPPHOLD where id = ANY(?::bigint[]);
            """.trimIndent()
        ) {
            setParams {
                setLongArray(1, utenlandsOppholdIds)
                setLong(2, behandlingId.id)
                setLongArray(3, utenlandsOppholdIds)
            }
        }
        log.info("Slettet $deletedRows rader fra OPPGITT_UTENLANDSOPPHOLD_GRUNNLAG")
    }
}
