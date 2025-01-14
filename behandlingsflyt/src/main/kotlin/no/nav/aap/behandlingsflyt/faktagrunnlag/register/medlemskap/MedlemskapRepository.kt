package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.adapter.MedlemskapResponse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import org.slf4j.LoggerFactory
import java.time.LocalDate

private val logger = LoggerFactory.getLogger(MedlemskapRepository::class.java)

class MedlemskapRepository(private val connection: DBConnection) {

    fun lagreUnntakMedlemskap(behandlingId: BehandlingId, unntak: List<MedlemskapResponse>) {
        if (hentHvisEksisterer(behandlingId) != null) {
            logger.info("Medlemsskapsgrunnlag for behandling $behandlingId eksisterer allerede. Deaktiverer forrige lagrede.")
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

        unntak.forEach { it ->
            connection.execute(
                """
                INSERT INTO MEDLEMSKAP_UNNTAK (STATUS, STATUS_ARSAK, MEDLEM, PERIODE, GRUNNLAG, LOVVALG, HELSEDEL, MEDLEMSKAP_UNNTAK_PERSON_ID, LOVVALGSLAND) VALUES (?, ?, ?, ?::daterange ,?, ?, ?, ?, ?)
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
                }
            }
        }
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
                )
                Segment(
                    it.getPeriode("PERIODE"),
                    unntak
                )
            }
        }.toList()
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): MedlemskapUnntakGrunnlag? {
        val behandlingsMedlemskapUnntak = connection.queryFirstOrNull(
            "SELECT MEDLEMSKAP_UNNTAK_PERSON_ID FROM MEDLEMSKAP_UNNTAK_GRUNNLAG WHERE BEHANDLING_ID=? AND AKTIV=TRUE"
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { it.getLong("MEDLEMSKAP_UNNTAK_PERSON_ID") }
        }
        if (behandlingsMedlemskapUnntak == null) {
            logger.info("Fant ingen aktive unntak for behandling med ID $behandlingId.")
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
}