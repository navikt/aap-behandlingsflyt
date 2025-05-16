package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.medlemsskap

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.KildesystemKode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.KildesystemMedl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapDataIntern
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.Unntak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.repository.RepositoryFactory
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import org.slf4j.LoggerFactory
import java.time.LocalDate

class MedlemskapRepositoryImpl(private val connection: DBConnection) : MedlemskapRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun lagreUnntakMedlemskap(behandlingId: BehandlingId, unntak: List<MedlemskapDataIntern>): Long {
        if (hentHvisEksisterer(behandlingId) != null) {
            log.info("Medlemsskapsgrunnlag for behandling $behandlingId eksisterer allerede. Deaktiverer forrige lagrede.")
            deaktiverEksisterendeGrunnlag(behandlingId)
        }

        val medlemskapUnntakPersonId = connection.executeReturnKey(
            """
            INSERT INTO MEDLEMSKAP_UNNTAK_PERSON DEFAULT VALUES
        """.trimIndent()
        )

        connection.execute(
            """
            INSERT INTO MEDLEMSKAP_UNNTAK_GRUNNLAG (BEHANDLING_ID, MEDLEMSKAP_UNNTAK_PERSON_ID) VALUES (?, ?)
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
                INSERT INTO MEDLEMSKAP_UNNTAK (
                STATUS, STATUS_ARSAK, MEDLEM, PERIODE, GRUNNLAG, LOVVALG, HELSEDEL, MEDLEMSKAP_UNNTAK_PERSON_ID, LOVVALGSLAND, KILDESYSTEM, KILDENAVN
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
            """SELECT * FROM MEDLEMSKAP_UNNTAK WHERE MEDLEMSKAP_UNNTAK_PERSON_ID = ?""".trimIndent()
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

    private fun hentKildesystem(row: Row): KildesystemMedl? {
        val kildesystemKode: KildesystemKode? = row.getEnumOrNull("KILDESYSTEM")
        val kildeNavn = row.getStringOrNull("KILDENAVN")

        return if (kildesystemKode != null && kildeNavn != null) {
            KildesystemMedl(kildesystemKode, kildeNavn)
        } else null
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): MedlemskapUnntakGrunnlag? {
        val behandlingsMedlemskapUnntak = connection.queryFirstOrNull(
            "SELECT MEDLEMSKAP_UNNTAK_PERSON_ID FROM MEDLEMSKAP_UNNTAK_GRUNNLAG WHERE BEHANDLING_ID=? AND AKTIV=TRUE"
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { it.getLong("MEDLEMSKAP_UNNTAK_PERSON_ID") }
        }
        if (behandlingsMedlemskapUnntak == null) {
            log.info("Fant ingen aktive unntak for behandling med ID $behandlingId.")
            return null
        }
        return MedlemskapUnntakGrunnlag(hentMedlemskapUnntak(behandlingsMedlemskapUnntak))
    }

    private fun deaktiverEksisterendeGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE MEDLEMSKAP_UNNTAK_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val medlemskapUnntakPersonIds = getMedlemskapUnntakPersonIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated(
            """
            delete from MEDLEMSKAP_UNNTAK_GRUNNLAG where behandling_id = ?; 
            delete from MEDLEMSKAP_UNNTAK_PERSON where id = ANY(?::bigint[]);
            delete from MEDLEMSKAP_UNNTAK where medlemskap_unntak_person_id = ANY(?::bigint[]);
          
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, medlemskapUnntakPersonIds)
                setLongArray(3, medlemskapUnntakPersonIds)

            }
        }
        log.info("Slettet $deletedRows fra MEDLEMSKAP_UNNTAK_GRUNNLAG")
    }

    private fun getMedlemskapUnntakPersonIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT medlemskap_unntak_person_id
                    FROM MEDLEMSKAP_UNNTAK_GRUNNLAG
                    WHERE behandling_id = ?
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("medlemskap_unntak_person_id")
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        log.warn("mangler kopier-metode for $javaClass. Skal denne klassen ha kopier-metode?")
    }

    companion object : RepositoryFactory<MedlemskapRepository> {
        override fun konstruer(connection: DBConnection): MedlemskapRepository {
            return MedlemskapRepositoryImpl(connection)

        }
    }
}