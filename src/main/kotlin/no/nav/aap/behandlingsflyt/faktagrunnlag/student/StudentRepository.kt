package no.nav.aap.behandlingsflyt.faktagrunnlag.student

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.student.StudentVurdering
import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.behandling.dokumenter.JournalpostId
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row

class StudentRepository(private val connection: DBConnection) {

    fun lagre(behandlingId: BehandlingId, studentvurdering: StudentVurdering?) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = StudentGrunnlag(
            null,
            studentvurdering = eksisterendeGrunnlag?.studentvurdering,
        )

        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverGrunnlag(behandlingId)
            }

            val vurderingId = lagreVurdering(studentvurdering)
            lagreGrunnlag(behandlingId, vurderingId)
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

    private fun lagreGrunnlag(behandlingId: BehandlingId, vurderingId: Long?) {
        val query = """
            INSERT INTO STUDENT_GRUNNLAG (behandling_id, student_id) VALUES (?, ?)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingId)
            }
        }
    }

    private fun lagreVurdering(studentvurdering: StudentVurdering?): Long? {
        if (studentvurdering == null) {
            return null
        }
        val query = """
                INSERT INTO STUDENT_VURDERING (begrunnelse, oppfylt, avbrutt_dato)
                VALUES (?, ?, ?)
            """.trimIndent()

        val vurderingId = connection.executeReturnKey(query) {
            setParams {
                setString(1, studentvurdering.begrunnelse)
                setBoolean(2, studentvurdering.oppfyller11_14)
                setLocalDate(3, studentvurdering.avbruttStudieDato)
            }
        }

        studentvurdering.dokumenterBruktIVurdering.forEach { lagreDokument(vurderingId, it) }

        return vurderingId
    }

    private fun lagreDokument(vurderingId: Long, journalpostId: JournalpostId) {
        val query = """
            INSERT INTO STUDENT_VURDERING_DOKUMENTER (vurdering_id, journalpost) 
            VALUES (?, ?)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, vurderingId)
                setString(2, journalpostId.identifikator)
            }
        }
    }

    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val hentHvisEksisterer = hentHvisEksisterer(fraBehandling)
        if (hentHvisEksisterer == null) {
            return
        }

        val query = """
            INSERT INTO STUDENT_GRUNNLAG (behandling_id, student_id) 
            SELECT ?, student_id from STUDENT_GRUNNLAG where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): StudentGrunnlag? {
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
            mapStudentVurdering(row.getLongOrNull("student_id"))
        )
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
                    it.getString("begrunnelse"),
                    hentDokumenterTilVurdering(studentId),
                    it.getBooleanOrNull("oppfylt"),
                    it.getLocalDateOrNull("avbrutt_dato")
                )
            }
        }
    }

    private fun hentDokumenterTilVurdering(studentId: Long): List<JournalpostId> {
        val query = """
            SELECT journalpost FROM STUDENT_VURDERING_DOKUMENTER WHERE vurdering_id = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, studentId)
            }
            setRowMapper {
                JournalpostId(it.getString("journalpost"))
            }
        }
    }

    fun hent(behandlingId: BehandlingId): StudentGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }
}
