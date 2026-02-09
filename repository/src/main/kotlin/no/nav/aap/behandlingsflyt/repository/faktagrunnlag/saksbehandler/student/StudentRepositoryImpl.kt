package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.student

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.OppgittStudent
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class StudentRepositoryImpl(private val connection: DBConnection) : StudentRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<StudentRepositoryImpl> {
        override fun konstruer(connection: DBConnection): StudentRepositoryImpl {
            return StudentRepositoryImpl(connection)
        }
    }

    override fun lagre(behandlingId: BehandlingId, oppgittStudent: OppgittStudent?) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = StudentGrunnlag(
            vurderinger = eksisterendeGrunnlag?.vurderinger,
            oppgittStudent = oppgittStudent
        )

        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverGrunnlag(behandlingId)
            }

            val oppgittStudentId = lagreOppgittStudent(oppgittStudent)

            lagreGrunnlag(behandlingId, eksisterendeGrunnlag?.vurderinger, oppgittStudentId)
        }
    }

    private fun lagreOppgittStudent(oppgittStudent: OppgittStudent?): Long? {
        if (oppgittStudent == null) {
            return null
        }
        val query = """
                INSERT INTO OPPGITT_STUDENT (avbrutt_dato, er_student, skal_gjenoppta_studie)
                VALUES ( ?, ?, ?)
            """.trimIndent()

        return connection.executeReturnKey(query) {
            setParams {
                setLocalDate(1, oppgittStudent.avbruttDato) // TODO: Få inn strukturert
                setEnumName(2, oppgittStudent.erStudentStatus)
                setEnumName(3, oppgittStudent.skalGjenopptaStudieStatus)
            }
        }
    }

    override fun lagre(behandlingId: BehandlingId, vurderinger: Set<StudentVurdering>?) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = StudentGrunnlag(
            vurderinger = vurderinger,
            oppgittStudent = eksisterendeGrunnlag?.oppgittStudent
        )

        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverGrunnlag(behandlingId)
            }

            lagreGrunnlag(behandlingId, vurderinger, eksisterendeGrunnlag?.oppgittStudent?.id)
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val studentVurderingerIds = getStudentIds(behandlingId)
        val oppgittStudentIds = getOppgittStudentIds(behandlingId)
        val deletedRows = connection.executeReturnUpdated(
            """
            delete from student_grunnlag where behandling_id = ?;
            delete from student_vurdering where student_vurderinger_id = any(?::bigint[]);
            delete from student_vurderinger where id = ANY(?::bigint[]);
            delete from oppgitt_student where id = ANY(?::bigint[]);
         
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, studentVurderingerIds)
                setLongArray(3, studentVurderingerIds)
                setLongArray(4, oppgittStudentIds)
            }
        }
        log.info("Slettet $deletedRows rader fra STUDENT_GRUNNLAG")
    }

    private fun getStudentIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
               SELECT student_vurderinger_id
                    FROM student_grunnlag
                    WHERE behandling_id = ? AND student_vurderinger_id is not null;
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("student_vurderinger_id")
        }
    }

    private fun getOppgittStudentIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT oppgitt_student_id
                    FROM STUDENT_GRUNNLAG
                    WHERE behandling_id = ? AND oppgitt_student_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("oppgitt_student_id")
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE STUDENT_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    private fun lagreGrunnlag(
        behandlingId: BehandlingId,
        vurderinger: Set<StudentVurdering>?,
        oppgittStudentId: Long?
    ) {
        val (vurderingId, vurderingerId) = when {
            vurderinger.isNullOrEmpty() -> Pair(null, null)
            else -> lagreVurdering(vurderinger.single())
        }

        val query = """
            INSERT INTO STUDENT_GRUNNLAG (behandling_id, student_vurderinger_id, oppgitt_student_id) VALUES (?, ?, ?)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingerId)
                setLong(3, oppgittStudentId)
            }
        }
    }

    private fun lagreVurdering(studentvurdering: StudentVurdering): Pair<Long, Long> {
        val vurderingerId = connection.executeReturnKey("""INSERT INTO STUDENT_VURDERINGER DEFAULT VALUES""")

        val query = """
                INSERT INTO STUDENT_VURDERING (begrunnelse, avbrutt_studie, godkjent_studie_av_laanekassen, avbrutt_pga_sykdom_eller_skade, har_behov_for_behandling, avbrutt_dato, avbrudd_mer_enn_6_maaneder, vurdert_av, fom, tom, vurdert_i_behandling, student_vurderinger_id, vurdert_tidspunkt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

        val vurderingId = connection.executeReturnKey(query) {
            setParams {
                setString(1, studentvurdering.begrunnelse)
                setBoolean(2, studentvurdering.harAvbruttStudie)
                setBoolean(3, studentvurdering.godkjentStudieAvLånekassen)
                setBoolean(4, studentvurdering.avbruttPgaSykdomEllerSkade)
                setBoolean(5, studentvurdering.harBehovForBehandling)
                setLocalDate(6, studentvurdering.avbruttStudieDato)
                setBoolean(7, studentvurdering.avbruddMerEnn6Måneder)
                setString(8, studentvurdering.vurdertAv)
                setLocalDate(9, studentvurdering.fom)
                setLocalDate(10, studentvurdering.tom)
                setLong(11, studentvurdering.vurdertIBehandling?.toLong())
                setLong(12, vurderingerId)
                setLocalDateTime(13, studentvurdering.vurdertTidspunkt)
            }
        }

        return Pair(vurderingId, vurderingerId)
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val hentHvisEksisterer = hentHvisEksisterer(fraBehandling)
        if (hentHvisEksisterer == null) {
            return
        }

        val query = """
            INSERT INTO STUDENT_GRUNNLAG (behandling_id, oppgitt_student_id, student_vurderinger_id) 
            SELECT ?, oppgitt_student_id, student_vurderinger_id from STUDENT_GRUNNLAG where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): StudentGrunnlag? {
        val query = """
            SELECT * FROM STUDENT_GRUNNLAG WHERE behandling_id = ? AND aktiv = true
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                mapGrunnlag(it)
            }
        }
    }

    private fun mapGrunnlag(row: Row): StudentGrunnlag {
        return StudentGrunnlag(
            row.getLongOrNull("student_vurderinger_id")?.let(::mapStudentVurderinger),
            row.getLongOrNull("oppgitt_student_id")?.let(::mapOppgittStudent)
        )
    }

    private fun mapOppgittStudent(id: Long): OppgittStudent? {
        val query = """
            SELECT * FROM OPPGITT_STUDENT WHERE id = ?
        """.trimIndent()
        //HAR_AVBRUTT, avbrutt_dato

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, id)
            }
            setRowMapper {
                OppgittStudent(
                    it.getLong("id"),
                    it.getLocalDateOrNull("avbrutt_dato"),
                    it.getEnum("er_student"),
                    it.getEnumOrNull("skal_gjenoppta_studie")
                )
            }
        }
    }

    private fun mapStudentVurderinger(studentVurderingerId: Long): Set<StudentVurdering>? {
        val query = """
            SELECT * FROM STUDENT_VURDERING WHERE student_vurdering.student_vurderinger_id = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, studentVurderingerId)
            }
            setRowMapper(::studentvurderingRowMapper)
        }.toSet()
    }

    private fun studentvurderingRowMapper(row: Row): StudentVurdering {
        return StudentVurdering(
            row.getLocalDate("fom"),
            row.getLocalDateOrNull("tom"),
            row.getString("begrunnelse"),
            row.getBoolean("avbrutt_studie"),
            row.getBooleanOrNull("godkjent_studie_av_laanekassen"),
            row.getBooleanOrNull("avbrutt_pga_sykdom_eller_skade"),
            row.getBooleanOrNull("har_behov_for_behandling"),
            row.getLocalDateOrNull("avbrutt_dato"),
            row.getBooleanOrNull("avbrudd_mer_enn_6_maaneder"),
            row.getString("vurdert_av"),
            row.getLocalDateTime("vurdert_tidspunkt"),
            row.getLong("vurdert_i_behandling").let(::BehandlingId),
        )
    }

    override fun hent(behandlingId: BehandlingId): StudentGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }

}
