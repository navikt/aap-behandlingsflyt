package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.Soningsvurdering
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.tidslinje.Segment
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class InstitusjonsoppholdRepository(private val connection: DBConnection) {

    private fun hentOpphold(oppholdId: Long?): Oppholdene? {
        if (oppholdId == null) {
            return null
        }

        return Oppholdene(
            id = oppholdId, opphold = connection.queryList(
                """
                SELECT * FROM OPPHOLD WHERE OPPHOLD_PERSON_ID =?
            """.trimIndent()
            ) {
                setParams {
                    setLong(1, oppholdId)
                }
                setRowMapper {
                    val institusjonsopphold = Institusjon(
                        Institusjonstype.valueOf(it.getString("INSTITUSJONSTYPE")),
                        Oppholdstype.valueOf(it.getString("KATEGORI")),
                        it.getString("ORGNR"),
                        it.getString("INSTITUSJONSNAVN")
                    )
                    Segment(
                        it.getPeriode("PERIODE"), institusjonsopphold
                    )
                }
            }.toList()
        )
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): InstitusjonsoppholdGrunnlag? {
        val keychain = connection.queryFirstOrNull(
            "SELECT OPPHOLD_PERSON_ID, soning_vurderinger_id FROM OPPHOLD_GRUNNLAG WHERE BEHANDLING_ID=? AND AKTIV=TRUE"
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                Keychain(it.getLongOrNull("OPPHOLD_PERSON_ID"), it.getLongOrNull("soning_vurderinger_id"))
            }
        }
        if (keychain == null) {
            return null
        }
        return InstitusjonsoppholdGrunnlag(
            hentOpphold(keychain.oppholdId), hentSoningsvurderinger(keychain.soningvurderingId)
        )
    }

    private fun hentSoningsvurderinger(soningsvurderingerId: Long?): Soningsvurderinger? {
        if (soningsvurderingerId == null) {
            return null
        }

        val vurderingene = connection.queryList(
            """
                SELECT * FROM SONING_VURDERING WHERE SONING_VURDERINGER_ID =?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, soningsvurderingerId)
            }
            setRowMapper { row ->
                Soningsvurdering(
                    skalOpphøre = row.getBoolean("SKAL_OPPHORE"),
                    fraDato = row.getLocalDate("FRA_DATO"),
                    begrunnelse = row.getString("BEGRUNNELSE")
                )
            }
        }.toList()

        return Soningsvurderinger(id = soningsvurderingerId, vurderinger = vurderingene)
    }

    fun hent(behandlingId: BehandlingId): InstitusjonsoppholdGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }

    fun lagreOpphold(behandlingId: BehandlingId, institusjonsopphold: List<Institusjonsopphold>) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        if (eksisterendeGrunnlag != null) {
            deaktiverEksisterendeGrunnlag(behandlingId)
        }
        val oppholdPersonId = connection.executeReturnKey(
            """
            INSERT INTO OPPHOLD_PERSON DEFAULT VALUES
        """.trimIndent()
        )

        connection.executeBatch(
            """
                INSERT INTO OPPHOLD (INSTITUSJONSTYPE, KATEGORI, ORGNR, PERIODE, OPPHOLD_PERSON_ID, INSTITUSJONSNAVN) VALUES (?, ?, ?, ?::daterange, ?, ?)
            """.trimIndent(), institusjonsopphold
        ) {
            setParams { opphold ->
                setString(1, opphold.institusjonstype.name)
                setString(2, opphold.kategori.name)
                setString(3, opphold.orgnr)
                setPeriode(4, opphold.periode())
                setLong(5, oppholdPersonId)
                setString(6, opphold.institusjonsnavn)
            }
        }

        connection.execute(
            """
            INSERT INTO OPPHOLD_GRUNNLAG (BEHANDLING_ID, OPPHOLD_PERSON_ID, soning_vurderinger_id) VALUES (?, ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, oppholdPersonId)
                setLong(3, eksisterendeGrunnlag?.soningsVurderinger?.id)
            }
        }
    }

    fun lagreSoningsVurdering(behandlingId: BehandlingId, soningsvurderinger: List<Soningsvurdering>) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeGrunnlag != null) {
            deaktiverEksisterendeGrunnlag(behandlingId)
        }

        val vurderingerId = lagreSoningsVurderinger(soningsvurderinger)
        connection.execute(
            """
            INSERT INTO OPPHOLD_GRUNNLAG (BEHANDLING_ID, OPPHOLD_PERSON_ID, soning_vurderinger_id) VALUES (?, ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, eksisterendeGrunnlag?.oppholdene?.id)
                setLong(3, vurderingerId)
            }
        }
    }

    private fun lagreSoningsVurderinger(soningsvurderings: List<Soningsvurdering>): Long? {
        if (soningsvurderings.isEmpty()) {
            return null
        }

        val vurderingerId = connection.executeReturnKey(
            """
            INSERT INTO SONING_VURDERINGER DEFAULT VALUES
        """.trimIndent()
        )

        val query = """
            INSERT INTO SONING_VURDERING (SONING_VURDERINGER_ID, SKAL_OPPHORE, BEGRUNNELSE, FRA_DATO) VALUES (?, ?, ?, ?)
        """.trimIndent()

        connection.executeBatch(query, soningsvurderings) {
            setParams {
                setLong(1, vurderingerId)
                setBoolean(2, it.skalOpphøre)
                setString(3, it.begrunnelse)
                setLocalDate(4, it.fraDato)
            }
        }

        return vurderingerId
    }

    fun lagreHelseVurdering(behandlingId: BehandlingId, helseinstitusjonVurderinger: List<HelseinstitusjonVurdering>) {

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
        val query = """INSERT INTO OPPHOLD_GRUNNLAG (BEHANDLING_ID, OPPHOLD_PERSON_ID, soning_vurderinger_id) 
            SELECT ?, OPPHOLD_PERSON_ID, soning_vurderinger_id 
            FROM OPPHOLD_GRUNNLAG 
            WHERE AKTIV AND BEHANDLING_ID = ?""".trimMargin()
        connection.execute(
            query
        ) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    internal data class Keychain(val oppholdId: Long?, val soningvurderingId: Long?)
}