package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection

class PipRepository(private val connection: DBConnection) {
    class IdentPåSak(
        val ident: String,
        val opprinnelse: Opprinnelse
    ) {
        enum class Opprinnelse {
            PERSON, BARN
        }
    }

    fun finnIdenterPåSak(saksnummer: Saksnummer): List<IdentPåSak> {
        val grunnlag = connection.queryList(
            """
            SELECT pi.IDENT AS IDENT, '${IdentPåSak.Opprinnelse.PERSON}' AS OPPRINNELSE
            FROM PERSON_IDENT pi
            INNER JOIN SAK s ON pi.person_id = s.person_id 
            WHERE SAKSNUMMER = ?
            UNION
            SELECT bo.IDENT as IDENT, '${IdentPåSak.Opprinnelse.BARN}' AS OPPRINNELSE
            FROM BARNOPPLYSNING bo
            INNER JOIN BARNOPPLYSNING_GRUNNLAG g ON bo.bgb_id = g.register_barn_id
            INNER JOIN BEHANDLING b ON g.BEHANDLING_ID = b.ID
            INNER JOIN SAK s ON b.SAK_ID = s.ID
            WHERE g.AKTIV AND s.SAKSNUMMER = ?
            UNION 
            SELECT ob.IDENT as IDENT, '${IdentPåSak.Opprinnelse.BARN}' AS OPPRINNELSE
            FROM OPPGITT_BARN ob
            INNER JOIN BARNOPPLYSNING_GRUNNLAG g ON ob.oppgitt_barn_id = g.oppgitt_barn_id
            INNER JOIN BEHANDLING b ON g.BEHANDLING_ID = b.ID
            INNER JOIN SAK s ON b.SAK_ID = s.ID
            WHERE g.AKTIV AND s.SAKSNUMMER = ?
            UNION 
            SELECT bv.IDENT as IDENT, '${IdentPåSak.Opprinnelse.BARN}' AS OPPRINNELSE
            FROM BARN_VURDERING bv
            INNER JOIN BARNOPPLYSNING_GRUNNLAG g ON bv.barn_vurderinger_id = g.vurderte_barn_id
            INNER JOIN BEHANDLING b ON g.BEHANDLING_ID = b.ID
            INNER JOIN SAK s ON b.SAK_ID = s.ID
            WHERE g.AKTIV AND s.SAKSNUMMER = ?
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
}
