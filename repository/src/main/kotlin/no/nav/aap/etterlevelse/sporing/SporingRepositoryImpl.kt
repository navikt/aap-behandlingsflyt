package no.nav.aap.etterlevelse.sporing

import com.fasterxml.jackson.databind.JsonNode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.repository.RepositoryFactory

class SporingRepositoryImpl(
    private val connection: DBConnection,
) : SporingRepository {
    override fun lagre(sporing: Sporing) {
        connection.execute(
            """
            insert into sporing(opprettet_tid, behandling_id, sporingsnokkel, versjon, grunnlag) 
            values (?, ?, ?, ?, ?::jsonb)
            on conflict (behandling_id, sporingsnokkel)
            do update set
                opprettet_tid = excluded.opprettet_tid,
                versjon = excluded.versjon,
                grunnlag = excluded.grunnlag
            """
        ) {
            setParams {
                setInstant(1, sporing.opprettetTidspunkt)
                setLong(2, sporing.behandlingId.toLong())
                setString(3, sporing.sporingsnøkkel.nøkkel)
                setString(4, sporing.versjon)
                setString(5, sporing.grunnlag.toString())
            }
        }
    }

    override fun hent(
        behandlingId: BehandlingId,
        sporingsnøkkel: Sporingsnøkkel
    ): Sporing? {
        return connection.queryFirstOrNull(
            """ select * from sporing where behandling_id = ? and sporingsnokkel = ?"""
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setString(2, sporingsnøkkel.nøkkel)
            }
            setRowMapper {
                Sporing(
                    behandlingId = behandlingId,
                    opprettetTidspunkt = it.getInstant("opprettet_tid"),
                    sporingsnøkkel = Sporingsnøkkel(it.getString("sporingsnokkel")),
                    versjon = it.getString("versjon"),
                    grunnlag = DefaultJsonMapper.fromJson<JsonNode>(it.getString("grunnlag")),
                )
            }
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        /* kopierer ikke sporing mellom behandlinger – sporing er per behandling. */
    }

    override fun slett(behandlingId: BehandlingId) {
        connection.execute("""delete from sporing where behandling_id = ?""") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }
    }

    companion object: RepositoryFactory<SporingRepositoryImpl> {
        override fun konstruer(connection: DBConnection): SporingRepositoryImpl {
            return SporingRepositoryImpl(connection)
        }
    }
}