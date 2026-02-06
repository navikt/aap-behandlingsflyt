package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.rettighetstype

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeFaktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.Factory

class RettighetstypeRepositoryImpl(private val connection: DBConnection) : RettighetstypeRepository {

    companion object : Factory<RettighetstypeRepositoryImpl> {
        override fun konstruer(connection: DBConnection): RettighetstypeRepositoryImpl {
            return RettighetstypeRepositoryImpl(connection)
        }
    }

    override fun hent(behandlingId: BehandlingId): RettighetstypeGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId)) { "Fant ikke rettighetstypegrunnlag for behandlingId=$behandlingId" }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): RettighetstypeGrunnlag? {
        val query = """
            select * from rettighetstype_grunnlag where behandling_id = ? and aktiv = true
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { mapGrunnlag(it) }
        }
    }

    private fun mapGrunnlag(row: Row): RettighetstypeGrunnlag {
        val perioderId = row.getLong("perioder_id")
        val query = """
            select * from rettighetstype_periode where perioder_id = ? order by periode
        """.trimIndent()
        val segmenter = connection.querySet(query) {
            setParams { setLong(1, perioderId) }
            setRowMapper { mapSegment(it) }
        }.sortedBy { it.periode.fom }
        
        return RettighetstypeGrunnlag(Tidslinje(segmenter))
    }

    private fun mapSegment(row: Row): Segment<RettighetsType> {
        return Segment(
            periode = row.getPeriode("periode"),
            verdi = RettighetsType.valueOf(row.getString("rettighetstype")),
        )
    }

    override fun lagre(
        behandlingId: BehandlingId,
        rettighetstypeTidslinje: Tidslinje<RettighetsType>,
        faktagrunnlag: RettighetstypeFaktagrunnlag,
        versjon: String,
    ) {
        val eksisterende = hentHvisEksisterer(behandlingId)
        val nyePerioder = RettighetstypeGrunnlag(rettighetstypeTidslinje)

        if (eksisterende != nyePerioder) {
            if (eksisterende != null) {
                deaktiverGrunnlag(behandlingId)
            }
            val grunnlagId = lagreNyttGrunnlag(behandlingId, nyePerioder)
            lagreSporing(grunnlagId, faktagrunnlag.hent(), versjon)
        }
    }

    private fun lagreSporing(grunnlagId: Long, faktagrunnlag: String?, versjon: String) {
        if (faktagrunnlag != null) {
            connection.execute(
                """
                insert into rettighetstype_sporing(rettighetstype_grunnlag_id, faktagrunnlag, versjon) 
                values (?, ?, ?)
                """.trimIndent()
            ) {
                setParams {
                    setLong(1, grunnlagId)
                    setString(2, faktagrunnlag)
                    setString(3, versjon)
                }
            }
        }
    }

    private fun lagreNyttGrunnlag(
        behandlingId: BehandlingId,
        grunnlag: RettighetstypeGrunnlag
    ): Long {
        val perioderId = connection.executeReturnKey("insert into rettighetstype_perioder default values")
        val query = """
            insert into rettighetstype_periode (perioder_id, periode, rettighetstype)
            values (?, ?::daterange, ?)
        """.trimIndent()
        connection.executeBatch(query, grunnlag.rettighetstypeTidslinje.segmenter()) {
            setParams { segment ->
                setLong(1, perioderId)
                setPeriode(2, segment.periode)
                setString(3, segment.verdi.name)
            }
        }
        val grunnlagQuery = """
            insert into rettighetstype_grunnlag (behandling_id, perioder_id, aktiv)
            values (?, ?, true)
        """.trimIndent()

        return connection.executeReturnKey(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, perioderId)
            }
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute(
            "update rettighetstype_grunnlag set aktiv = false where behandling_id = ? and aktiv = true"
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setResultValidator { require(it == 1) }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        hentHvisEksisterer(fraBehandling) ?: return

        val query = """
            insert into rettighetstype_grunnlag (behandling_id, perioder_id, aktiv)
            select ?, perioder_id, true from rettighetstype_grunnlag where behandling_id = ? and aktiv
        """.trimIndent()
        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        slettMedCount(behandlingId)
    }

    fun hentSporingHvisEksisterer(behandlingId: BehandlingId): Pair<String, String>? {
        val query = """
            select s.* from rettighetstype_sporing s
            join rettighetstype_grunnlag g on s.rettighetstype_grunnlag_id = g.id
            where g.behandling_id = ? and g.aktiv = true
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper {
                Pair(
                    it.getString("versjon"),
                    it.getString("faktagrunnlag")
                )
            }
        }
    }

    fun slettMedCount(behandlingId: BehandlingId): Int {
        val perioderIds = connection.queryList(
            "select perioder_id from rettighetstype_grunnlag where behandling_id = ?",
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { it.getLong("perioder_id") }
        }

        val slettetSporing = connection.executeReturnUpdated(
            """
        delete from rettighetstype_sporing s where s.rettighetstype_grunnlag_id in (select id from rettighetstype_grunnlag where behandling_id = ?);
              """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
        }

        val slettetGrunnlag = connection.executeReturnUpdated(
            """
        delete from rettighetstype_grunnlag where behandling_id = ?;
        """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
        }

        val slettedePerioder = connection.executeReturnUpdated(
            """
        delete from rettighetstype_periode where perioder_id = any(?::bigint[]);
        """.trimIndent()
        ) {
            setParams { setLongArray(1, perioderIds) }
        }


        val slettetPerioder = connection.executeReturnUpdated(
            """
        delete from rettighetstype_perioder where id = any(?::bigint[]);
        """.trimIndent()
        ) {
            setParams { setLongArray(1, perioderIds) }
        }

        return slettetSporing + slettetGrunnlag + slettedePerioder + slettetPerioder
    }
}