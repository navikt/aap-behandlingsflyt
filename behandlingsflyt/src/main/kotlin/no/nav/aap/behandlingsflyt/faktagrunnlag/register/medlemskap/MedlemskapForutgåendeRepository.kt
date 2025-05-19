package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.repository.RepositoryFactory
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Repository
import org.slf4j.LoggerFactory
import java.time.LocalDate

interface MedlemskapForutgåendeRepository : Repository {
    fun lagreUnntakMedlemskap(behandlingId: BehandlingId, unntak: List<MedlemskapDataIntern>): Long
    fun hentHvisEksisterer(behandlingId: BehandlingId): MedlemskapUnntakGrunnlag?
    override fun slett(behandlingId: BehandlingId)
}

class MedlemskapForutgåendeRepositoryImpl(private val connection: DBConnection) : MedlemskapForutgåendeRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun lagreUnntakMedlemskap(behandlingId: BehandlingId, unntak: List<MedlemskapDataIntern>): Long {
        if (hentHvisEksisterer(behandlingId) != null) {
            deaktiverEksisterendeGrunnlag(behandlingId)
        }

        val medlemskapUnntakPersonId = connection.executeReturnKey(
            """
            INSERT INTO MEDLEMSKAP_FORUTGAAENDE_UNNTAK_PERSON DEFAULT VALUES
        """.trimIndent()
        )

        connection.execute(
            """
            INSERT INTO MEDLEMSKAP_FORUTGAAENDE_UNNTAK_GRUNNLAG (BEHANDLING_ID, MEDLEMSKAP_FORUTGAAENDE_UNNTAK_PERSON_ID) VALUES (?, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, medlemskapUnntakPersonId)
            }
        }

        unntak.forEach {
            connection.execute(
                """
                INSERT INTO MEDLEMSKAP_FORUTGAAENDE_UNNTAK (
                STATUS, STATUS_ARSAK, MEDLEM, PERIODE, GRUNNLAG, LOVVALG, HELSEDEL, MEDLEMSKAP_FORUTGAAENDE_UNNTAK_PERSON_ID, LOVVALGSLAND, KILDESYSTEM, KILDENAVN
                ) VALUES (?, ?, ?, ?::daterange ,?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ) {
                setParams {
                    setString(1, it.status)
                    setString(2, it.statusaarsak)
                    setBoolean(3, it.medlem)
                    setPeriode(4, Periode(LocalDate.parse(it.fraOgMed), LocalDate.parse(it.tilOgMed)))
                    setString(5, it.grunnlag)
                    setString(6, it.lovvalg)
                    setBoolean(7, it.helsedel)
                    setLong(8, medlemskapUnntakPersonId)
                    setString(9, it.lovvalgsland)
                    setEnumName(10, it.kilde?.kildesystemKode)
                    setString(11, it.kilde?.kildeNavn)
                }
            }
        }
        return medlemskapUnntakPersonId
    }

    private fun hentMedlemskapUnntak(behandlingsMedlemskapUnntak: Long): List<Segment<Unntak>> {
        return connection.queryList(
            """SELECT * FROM MEDLEMSKAP_FORUTGAAENDE_UNNTAK WHERE MEDLEMSKAP_FORUTGAAENDE_UNNTAK_PERSON_ID = ?""".trimIndent()
        ) {
            setParams {
                setLong(1, behandlingsMedlemskapUnntak)
            }
            setRowMapper {
                val unntak = Unntak(
                    status = it.getString("STATUS"),
                    statusaarsak = it.getStringOrNull("STATUS_ARSAK"),
                    medlem = it.getBoolean("MEDLEM"),
                    grunnlag = it.getString("GRUNNLAG"),
                    lovvalg = it.getString("LOVVALG"),
                    helsedel = it.getBoolean("HELSEDEL"),
                    lovvalgsland = it.getStringOrNull("LOVVALGSLAND"),
                    kilde = hentKildesystem(it)
                )
                Segment(
                    it.getPeriode("PERIODE"),
                    unntak
                )
            }
        }.toList()
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): MedlemskapUnntakGrunnlag? {
        val behandlingsMedlemskapUnntak = connection.queryFirstOrNull(
            "SELECT MEDLEMSKAP_FORUTGAAENDE_UNNTAK_PERSON_ID FROM MEDLEMSKAP_FORUTGAAENDE_UNNTAK_GRUNNLAG WHERE BEHANDLING_ID=? AND AKTIV=TRUE"
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { it.getLong("MEDLEMSKAP_FORUTGAAENDE_UNNTAK_PERSON_ID") }
        }
        if (behandlingsMedlemskapUnntak == null) {
            return null
        }
        return MedlemskapUnntakGrunnlag(hentMedlemskapUnntak(behandlingsMedlemskapUnntak))
    }

    private fun hentKildesystem(row: Row): KildesystemMedl? {
        val kildesystemKode: KildesystemKode? = row.getEnumOrNull("KILDESYSTEM")
        val kildeNavn = row.getStringOrNull("KILDENAVN")

        return if (kildesystemKode != null && kildeNavn != null) {
            KildesystemMedl(kildesystemKode, kildeNavn)
        } else null
    }

    private fun deaktiverEksisterendeGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE MEDLEMSKAP_FORUTGAAENDE_UNNTAK_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        hentHvisEksisterer(fraBehandling) ?: return

        require(fraBehandling != tilBehandling) { "Kan ikke kopiere medlemsskapgrunnlag til samme behandling" }

        connection.execute(
            """
            INSERT INTO MEDLEMSKAP_FORUTGAAENDE_UNNTAK_GRUNNLAG (behandling_id, MEDLEMSKAP_FORUTGAAENDE_UNNTAK_PERSON_ID)
            select ?, MEDLEMSKAP_FORUTGAAENDE_UNNTAK_PERSON_ID
            from MEDLEMSKAP_FORUTGAAENDE_UNNTAK_GRUNNLAG
            where behandling_id = ? and aktiv
        """.trimIndent()
        ) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val medlemskapForutgaaendeUnntakPersonIds = getMedlemskapForutgaaendeUnntakPersonIds(behandlingId)

        connection.execute(
            """
            delete from MEDLEMSKAP_FORUTGAAENDE_UNNTAK_GRUNNLAG where behandling_id = ?; 
            delete from MEDLEMSKAP_FORUTGAAENDE_UNNTAK where medlemskap_forutgaaende_unntak_person_id = ANY(?::bigint[]);
            delete from MEDLEMSKAP_FORUTGAAENDE_UNNTAK_PERSON where id = ANY(?::bigint[]);
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, medlemskapForutgaaendeUnntakPersonIds)
                setLongArray(3, medlemskapForutgaaendeUnntakPersonIds)
            }
        }
    }

    private fun getMedlemskapForutgaaendeUnntakPersonIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT medlemskap_forutgaaende_unntak_person_id
                    FROM MEDLEMSKAP_FORUTGAAENDE_UNNTAK_GRUNNLAG
                    WHERE behandling_id = ?
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("medlemskap_unntak_person_id")
        }
    }

    companion object : RepositoryFactory<MedlemskapForutgåendeRepository> {
        override fun konstruer(connection: DBConnection): MedlemskapForutgåendeRepository {
            return MedlemskapForutgåendeRepositoryImpl(connection)
        }

    }

}