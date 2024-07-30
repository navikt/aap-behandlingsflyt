package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.tidslinje.Segment
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.LocalDate

class InstitusjonsoppholdRepository(private val connection: DBConnection) {

    private fun hentOpphold(opphold_grunnlag_id: Long): List<Segment<Institusjon>> {
        return connection.queryList(
            """
                SELECT * FROM OPPHOLD WHERE OPPHOLD_PERSON_ID =?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, opphold_grunnlag_id)
            }
            setRowMapper {
                val institusjonsopphold = Institusjon(
                    Institusjonstype.valueOf(it.getString("INSTITUSJONSTYPE")),
                    Oppholdstype.valueOf(it.getString("KATEGORI")),
                    it.getString("ORGNR"),
                    it.getString("INSTITUSJONSNAVN")
                )
                Segment(
                    it.getPeriode("PERIODE"),
                    institusjonsopphold
                )
            }
        }.toList()
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): InstitusjonsoppholdGrunnlag? {
        val behandlings_opphold = connection.queryFirstOrNull(
            "SELECT OPPHOLD_PERSON_ID FROM OPPHOLD_GRUNNLAG WHERE BEHANDLING_ID=? AND AKTIV=TRUE"
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                it.getLong("OPPHOLD_PERSON_ID")
            }
        }
        if (behandlings_opphold == null) {
            return null
        }
        return InstitusjonsoppholdGrunnlag(hentOpphold(behandlings_opphold))
    }

    fun hent(behandlingId: BehandlingId): List<Segment<Institusjon>> {
        val behandlings_opphold = connection.queryFirstOrNull(
            "SELECT OPPHOLD_PERSON_ID FROM OPPHOLD_GRUNNLAG WHERE BEHANDLING_ID=? AND AKTIV=TRUE"
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                it.getLong("OPPHOLD_PERSON_ID")
            }
        }
        return hentOpphold(requireNotNull(behandlings_opphold))
    }

    fun lagreOpphold(behandlingId: BehandlingId, institusjonsopphold: List<Institusjonsopphold>) {
        if(hentHvisEksisterer(behandlingId) != null) {
            deaktiverEksisterendeGrunnlag(behandlingId)
        }
        val oppholdPersonId = connection.executeReturnKey(
            """
            INSERT INTO OPPHOLD_PERSON DEFAULT VALUES
        """.trimIndent()
        )

        connection.executeReturnKey(
            """
            INSERT INTO OPPHOLD_GRUNNLAG (BEHANDLING_ID, OPPHOLD_PERSON_ID) VALUES (?, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, oppholdPersonId)
            }
        }

        institusjonsopphold.forEach { opphold ->
            connection.execute(
                """
                INSERT INTO OPPHOLD (INSTITUSJONSTYPE, KATEGORI, ORGNR, PERIODE, OPPHOLD_PERSON_ID, INSTITUSJONSNAVN) VALUES (?, ?, ?, ?::daterange, ?, ?)
            """.trimIndent()
            ) {
                setParams {
                    setString(1, opphold.institusjonstype.name)
                    setString(2, opphold.kategori.name)
                    setString(3, opphold.orgnr)
                    setPeriode(4, Periode(opphold.startdato, opphold.sluttdato ?: LocalDate.MAX))
                    setLong(5, oppholdPersonId)
                    setString(6, opphold.institusjonsnavn)
                }
            }
        }


    }

    private fun deaktiverEksisterendeGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE OPPHOLD_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }


    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        connection.execute("INSERT INTO OPPHOLD_GRUNNLAG (BEHANDLING_ID, OPPHOLD_PERSON_ID) SELECT ?, OPPHOLD_PERSON_ID FROM OPPHOLD_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

}