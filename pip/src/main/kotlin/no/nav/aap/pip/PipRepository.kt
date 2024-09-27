package no.nav.aap.pip

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection

class PipRepository(private val connection: DBConnection) {
    class IdentPåSak(
        private val ident: String,
        private val opprinnelse: Opprinnelse
    ) {
        enum class Opprinnelse {
            PERSON, BARN
        }

        companion object {
            fun Iterable<IdentPåSak>.filterDistinctIdent(opprinnelse: Opprinnelse): List<String> {
                return this.filter { it.opprinnelse == opprinnelse }.map(IdentPåSak::ident).distinct()
            }
        }
    }

    fun finnIdenterPåSak(saksnummer: Saksnummer): List<IdentPåSak> {
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
            WHERE s.SAKSNUMMER = ?
            UNION 
            SELECT bv.IDENT as IDENT, '${IdentPåSak.Opprinnelse.BARN}' AS OPPRINNELSE
            FROM BARN_VURDERING bv
            INNER JOIN BARNOPPLYSNING_GRUNNLAG g ON bv.barn_vurderinger_id = g.vurderte_barn_id
            INNER JOIN BEHANDLING b ON g.BEHANDLING_ID = b.ID
            INNER JOIN SAK s ON b.SAK_ID = s.ID
            WHERE s.SAKSNUMMER = ?
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

    fun finnIdenterPåBehandling(behandlingReferanse: BehandlingReferanse): List<IdentPåSak> {
        val grunnlag = connection.queryList(
            """
            SELECT pi.IDENT AS IDENT, '${IdentPåSak.Opprinnelse.PERSON}' AS OPPRINNELSE
            FROM PERSON_IDENT pi
            INNER JOIN SAK s ON pi.person_id = s.person_id
            INNER JOIN BEHANDLING bRef ON s.id = bRef.sak_id
            WHERE bRef.referanse = ?
            UNION
            SELECT bo.IDENT as IDENT, '${IdentPåSak.Opprinnelse.BARN}' AS OPPRINNELSE
            FROM BARNOPPLYSNING bo
            INNER JOIN BARNOPPLYSNING_GRUNNLAG g ON bo.bgb_id = g.register_barn_id
            INNER JOIN BEHANDLING b ON g.BEHANDLING_ID = b.ID
            INNER JOIN BEHANDLING bRef ON b.SAK_ID = bRef.SAK_ID
            WHERE bRef.referanse = ?
            UNION 
            SELECT ob.IDENT as IDENT, '${IdentPåSak.Opprinnelse.BARN}' AS OPPRINNELSE
            FROM OPPGITT_BARN ob
            INNER JOIN BARNOPPLYSNING_GRUNNLAG g ON ob.oppgitt_barn_id = g.oppgitt_barn_id
            INNER JOIN BEHANDLING b ON g.BEHANDLING_ID = b.ID
            INNER JOIN BEHANDLING bRef ON b.SAK_ID = bRef.SAK_ID
            WHERE bRef.referanse = ?
            UNION 
            SELECT bv.IDENT as IDENT, '${IdentPåSak.Opprinnelse.BARN}' AS OPPRINNELSE
            FROM BARN_VURDERING bv
            INNER JOIN BARNOPPLYSNING_GRUNNLAG g ON bv.barn_vurderinger_id = g.vurderte_barn_id
            INNER JOIN BEHANDLING b ON g.BEHANDLING_ID = b.ID
            INNER JOIN BEHANDLING bRef ON b.SAK_ID = bRef.SAK_ID
            WHERE bRef.referanse = ?
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
}
