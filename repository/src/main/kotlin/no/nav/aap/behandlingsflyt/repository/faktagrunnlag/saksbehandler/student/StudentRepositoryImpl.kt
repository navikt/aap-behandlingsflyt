package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.student

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.OppgittStudent
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory

class StudentRepositoryImpl(private val connection: DBConnection) : StudentRepository {

    companion object : Factory<StudentRepositoryImpl> {
        override fun konstruer(connection: DBConnection): StudentRepositoryImpl {
            return StudentRepositoryImpl(connection)
        }
    }

    override fun lagre(behandlingId: BehandlingId, oppgittStudent: OppgittStudent?) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = StudentGrunnlag(
            null,
            studentvurdering = eksisterendeGrunnlag?.studentvurdering,
            oppgittStudent = oppgittStudent
        )

        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverGrunnlag(behandlingId)
            }

            val oppgittStudentId = lagreOppgittStudent(oppgittStudent)
            lagreGrunnlag(behandlingId, eksisterendeGrunnlag?.studentvurdering?.id, oppgittStudentId)
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

    override fun lagre(behandlingId: BehandlingId, studentvurdering: StudentVurdering?) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = StudentGrunnlag(
            null,
            studentvurdering = studentvurdering,
            oppgittStudent = eksisterendeGrunnlag?.oppgittStudent
        )

        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverGrunnlag(behandlingId)
            }

            val vurderingId = lagreVurdering(studentvurdering)
            lagreGrunnlag(behandlingId, vurderingId, eksisterendeGrunnlag?.oppgittStudent?.id)
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val studentIds = getStudentIds(behandlingId)
        val oppgittStudentIds = getOppgittStudentIds(behandlingId)
        connection.execute("""
            delete from STUDENT_GRUNNLAG where id = ?;
            delete from OPPGITT_STUDENT where id = ANY(?::bigint[]);
            delete from STUDENT_VURDERING where id = ANY(?::bigint[]);
          
            
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, oppgittStudentIds)
                setLongArray(3, studentIds)
            }
        }
    }

    private fun getStudentIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT student_id
                    FROM STUDENT_GRUNNLAG
                    WHERE behandling_id = ?
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("student_id")
        }
    }

    private fun getOppgittStudentIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT oppgitt_student_id
                    FROM STUDENT_GRUNNLAG
                    WHERE behandling_id = ?
                 
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

    private fun lagreGrunnlag(behandlingId: BehandlingId, vurderingId: Long?, oppgittStudentId: Long?) {
        val query = """
            INSERT INTO STUDENT_GRUNNLAG (behandling_id, student_id, oppgitt_student_id) VALUES (?, ?, ?)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingId)
                setLong(3, oppgittStudentId)
            }
        }
    }

    private fun lagreVurdering(studentvurdering: StudentVurdering?): Long? {
        if (studentvurdering == null) {
            return null
        }
        val query = """
                INSERT INTO STUDENT_VURDERING (begrunnelse, avbrutt_studie, godkjent_studie_av_laanekassen, avbrutt_pga_sykdom_eller_skade, har_behov_for_behandling, avbrutt_dato, avbrudd_mer_enn_6_maaneder)
                VALUES (?, ?, ?, ?, ?, ?, ?)
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
            }
        }

        return vurderingId
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val hentHvisEksisterer = hentHvisEksisterer(fraBehandling)
        if (hentHvisEksisterer == null) {
            return
        }

        val query = """
            INSERT INTO STUDENT_GRUNNLAG (behandling_id, student_id, oppgitt_student_id) 
            SELECT ?, student_id, oppgitt_student_id from STUDENT_GRUNNLAG where behandling_id = ? and aktiv
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
            row.getLong("id"),
            mapStudentVurdering(row.getLongOrNull("student_id")),
            mapOppgittStudent(row.getLongOrNull("oppgitt_student_id"))
        )
    }

    private fun mapOppgittStudent(id: Long?): OppgittStudent? {
        if (id == null) {
            return null
        }
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

    private fun mapStudentVurdering(studentId: Long?): StudentVurdering? {
        if (studentId == null) {
            return null
        }
        val query = """
            SELECT * FROM STUDENT_VURDERING WHERE id = ?
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, studentId)
            }
            setRowMapper {
                StudentVurdering(
                    it.getLong("id"),
                    it.getString("begrunnelse"),
                    it.getBoolean("avbrutt_studie"),
                    it.getBooleanOrNull("godkjent_studie_av_laanekassen"),
                    it.getBooleanOrNull("avbrutt_pga_sykdom_eller_skade"),
                    it.getBooleanOrNull("har_behov_for_behandling"),
                    it.getLocalDateOrNull("avbrutt_dato"),
                    it.getBooleanOrNull("avbrudd_mer_enn_6_maaneder"),
                )
            }
        }
    }

    override fun hent(behandlingId: BehandlingId): StudentGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }

}
