package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.rettighetstype

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KvoteVurdering
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeMedKvote
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
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
        val perioder = connection.querySet(query) {
            setParams { setLong(1, perioderId) }
            setRowMapper { mapPeriode(it) }
        }
        return RettighetstypeGrunnlag(perioder)
    }

    private fun mapPeriode(row: Row): RettighetstypeMedKvote {
        return RettighetstypeMedKvote(
            periode = row.getPeriode("periode"),
            rettighetstype = RettighetsType.valueOf(row.getString("rettighetstype")),
            avslagsårsaker = row.getArray("avslagsaarsaker_kvote", String::class)
                .map { Avslagsårsak.valueOf(it) }.toSet(),
            brukerAvKvoter = row.getArray("bruker_av_kvoter", String::class)
                .map { Kvote.valueOf(it) }.toSet()
        )
    }

    override fun lagre(
        behandlingId: BehandlingId,
        rettighetstypeMedKvoteVurderinger: Tidslinje<KvoteVurdering>,
        input: Faktagrunnlag
    ) {
        val eksisterende = hentHvisEksisterer(behandlingId)
        val nyePerioder = rettighetstypeMedKvoteVurderinger
            .segmenter()
            .filter { it.verdi.rettighetsType != null }
            .map {
                RettighetstypeMedKvote(
                    periode = it.periode,
                    rettighetstype = it.verdi.rettighetsType!!,
                    avslagsårsaker = it.verdi.avslagsårsaker(),
                    brukerAvKvoter = it.verdi.brukerAvKvoter()
                )
            }.toSet()
        if (eksisterende?.perioder != nyePerioder) {
            if (eksisterende != null) {
                deaktiverGrunnlag(behandlingId)
            }
            lagreNyttGrunnlag(behandlingId, nyePerioder)
        }
    }

    private fun lagreNyttGrunnlag(
        behandlingId: BehandlingId,
        perioder: Set<RettighetstypeMedKvote>
    ) {
        val perioderId = connection.executeReturnKey("insert into rettighetstype_perioder default values")
        val query = """
            insert into rettighetstype_periode (perioder_id, periode, rettighetstype, bruker_av_kvoter, avslagsaarsaker_kvote)
            values (?, ?::daterange, ?, ?, ?)
        """.trimIndent()
        connection.executeBatch(query, perioder) {
            setParams { periode ->
                setLong(1, perioderId)
                setPeriode(2, periode.periode)
                setString(3, periode.rettighetstype.name)
                setArray(4, periode.brukerAvKvoter.map { it.name })
                setArray(5, periode.avslagsårsaker.map { it.name })
            }
        }
        val grunnlagQuery = """
            insert into rettighetstype_grunnlag (behandling_id, perioder_id, aktiv)
            values (?, ?, true)
        """.trimIndent()
        connection.execute(grunnlagQuery) {
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
        val perioderIds = connection.queryList(
            "select perioder_id from rettighetstype_grunnlag where behandling_id = ?",
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { it.getLong("perioder_id") }
        }

        connection.execute("delete from rettighetstype_grunnlag where behandling_id = ?") {
            setParams { setLong(1, behandlingId.toLong()) }
        }

        if (perioderIds.isNotEmpty()) {
            connection.execute(
                "delete from rettighetstype_periode where perioder_id = any(?::bigint[])"
            ) {
                setParams { setLongArray(1, perioderIds) }
            }
            connection.execute(
                "delete from rettighetstype_perioder where id = any(?::bigint[])"
            ) {
                setParams { setLongArray(1, perioderIds) }
            }
        }
    }
}