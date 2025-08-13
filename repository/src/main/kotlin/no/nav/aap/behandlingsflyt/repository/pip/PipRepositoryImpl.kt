package no.nav.aap.behandlingsflyt.repository.pip

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.pip.IdentPåSak
import no.nav.aap.behandlingsflyt.pip.PipRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory

class PipRepositoryImpl(private val connection: DBConnection) : PipRepository {
    companion object : Factory<PipRepositoryImpl> {
        override fun konstruer(connection: DBConnection): PipRepositoryImpl {
            return PipRepositoryImpl(connection)
        }
    }

    override fun finnIdenterPåSak(saksnummer: Saksnummer): List<IdentPåSak> {
        val grunnlag = connection.queryList(
            """
            SELECT pi.IDENT AS IDENT, '${IdentPåSak.Opprinnelse.PERSON}' AS OPPRINNELSE
            FROM PERSON_IDENT pi
            INNER JOIN SAK s ON pi.person_id = s.person_id 
            WHERE s.SAKSNUMMER = ?
            UNION
            SELECT bo.IDENT as IDENT, '${IdentPåSak.Opprinnelse.BARN}' AS OPPRINNELSE
            FROM BARNOPPLYSNING bo
            INNER JOIN BARNOPPLYSNING_GRUNNLAG g ON bo.bgb_id = g.register_barn_id
            INNER JOIN BEHANDLING b ON g.BEHANDLING_ID = b.ID
            INNER JOIN SAK s ON b.SAK_ID = s.ID
            WHERE s.SAKSNUMMER = ?
            UNION 
            SELECT ob.IDENT as IDENT, '${IdentPåSak.Opprinnelse.BARN}' AS OPPRINNELSE
            FROM OPPGITT_BARN ob
            INNER JOIN BARNOPPLYSNING_GRUNNLAG g ON ob.oppgitt_barn_id = g.oppgitt_barn_id
            INNER JOIN BEHANDLING b ON g.BEHANDLING_ID = b.ID
            INNER JOIN SAK s ON b.SAK_ID = s.ID
            WHERE s.SAKSNUMMER = ? AND ob.ident is not null
            UNION 
            SELECT bv.IDENT as IDENT, '${IdentPåSak.Opprinnelse.BARN}' AS OPPRINNELSE
            FROM BARN_VURDERING bv
            INNER JOIN BARNOPPLYSNING_GRUNNLAG g ON bv.barn_vurderinger_id = g.vurderte_barn_id
            INNER JOIN BEHANDLING b ON g.BEHANDLING_ID = b.ID
            INNER JOIN SAK s ON b.SAK_ID = s.ID
            WHERE s.SAKSNUMMER = ? and bv.ident is not null
        """.trimIndent()
        ) {
            setParams {
                setString(1, saksnummer.toString())
                setString(2, saksnummer.toString())
                setString(3, saksnummer.toString())
                setString(4, saksnummer.toString())
            }
            setRowMapper { row ->
                IdentPåSak(
                    row.getString("IDENT"),
                    row.getEnum("OPPRINNELSE")
                )
            }
        }

        return grunnlag
    }

    override fun finnIdenterPåBehandling(behandlingReferanse: BehandlingReferanse): List<IdentPåSak> {
        val grunnlag = connection.queryList(
            """
            SELECT pi.IDENT AS IDENT, '${IdentPåSak.Opprinnelse.PERSON}' AS OPPRINNELSE
            FROM PERSON_IDENT pi
            INNER JOIN SAK s ON pi.person_id = s.person_id
            INNER JOIN BEHANDLING bRef ON s.id = bRef.sak_id
            WHERE bRef.referanse = ?
            UNION
            SELECT pi.ident AS IDENT, '${IdentPåSak.Opprinnelse.BARN}' AS OPPRINNELSE
            FROM barnopplysning bo
                     join person p on bo.person_id = p.id
                     join person_ident pi on p.id = pi.person_id
                     join barnopplysning_grunnlag bg on bg.register_barn_id = bo.id
                     join behandling b on bg.behandling_id = b.id
                     join behandling bRef on b.sak_id = bRef.sak_id
            WHERE bRef.referanse = ? and aktiv = true
            UNION
            SELECT ob.IDENT as IDENT, '${IdentPåSak.Opprinnelse.BARN}' AS OPPRINNELSE
            FROM OPPGITT_BARN ob
            INNER JOIN BARNOPPLYSNING_GRUNNLAG g ON ob.oppgitt_barn_id = g.oppgitt_barn_id
            INNER JOIN BEHANDLING b ON g.BEHANDLING_ID = b.ID
            INNER JOIN BEHANDLING bRef ON b.SAK_ID = bRef.SAK_ID
            WHERE bRef.referanse = ? and ob.ident is not null
            UNION 
            SELECT bv.IDENT as IDENT, '${IdentPåSak.Opprinnelse.BARN}' AS OPPRINNELSE
            FROM BARN_VURDERING bv
            INNER JOIN BARNOPPLYSNING_GRUNNLAG g ON bv.barn_vurderinger_id = g.vurderte_barn_id
            INNER JOIN BEHANDLING b ON g.BEHANDLING_ID = b.ID
            INNER JOIN BEHANDLING bRef ON b.SAK_ID = bRef.SAK_ID
            WHERE bRef.referanse = ? and bv.ident is not null
        """.trimIndent()
        ) {
            setParams {
                setUUID(1, behandlingReferanse.referanse)
                setUUID(2, behandlingReferanse.referanse)
                setUUID(3, behandlingReferanse.referanse)
                setUUID(4, behandlingReferanse.referanse)
            }
            setRowMapper { row ->
                IdentPåSak(
                    row.getString("IDENT"),
                    row.getEnum("OPPRINNELSE")
                )
            }
        }

        return grunnlag
    }

    override fun slett(behandlingId: BehandlingId) {
        // Ikke relevant for å slette trukket sak
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Denne trengs ikke implementeres
    }
}