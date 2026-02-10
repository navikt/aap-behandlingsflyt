package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Factory
import java.time.Instant

class EtableringEgenVirksomhetRepositoryImpl(private val connection: DBConnection) :
    EtableringEgenVirksomhetRepository {

    companion object : Factory<EtableringEgenVirksomhetRepositoryImpl> {
        override fun konstruer(connection: DBConnection): EtableringEgenVirksomhetRepositoryImpl {
            return EtableringEgenVirksomhetRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): EtableringEgenVirksomhetGrunnlag? {
        TODO("Not yet implemented")
    }

    override fun lagre(
        behandlingId: BehandlingId,
        etableringEgenvirksomhetVurderinger: List<EtableringEgenVirksomhetVurdering>
    ) {
        deaktiverGrunnlag(behandlingId)

        val query = """
            INSERT INTO ETABLERING_EGEN_VIRKSOMHET_VURDERINGER (OPPRETTET_TID)
            VALUES (?)
        """.trimIndent()
        val vurderingerId = connection.executeReturnKey(query)

        connection.executeBatch(
            """
            INSERT INTO ETABLERING_EGEN_VIRKSOMHET_VURDERING (BEGRUNNELSE, FORELIGGER_FAGLIG_VURDERING, VIRKSOMHET_ER_NY, BRUKER_EIER_VIRKSOMHET, KAN_BLI_SELVFORSORGET, VIRKSOMHET_NAVN, ORG_NR, EGEN_VIRKSOMHET_UTVIKLING_PERIODER_ID, EGEN_VIRKSOMHET_OPPSTART_PERIODER_ID, VURDERINGER_ID, VURDERT_I_BEHANDLING, VURDERT_AV, GJELDER_FRA, GJELDER_TIL, OPPRETTET_TID)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ,? ,?, ?)
        """.trimIndent(), etableringEgenvirksomhetVurderinger
        ) {
            setParams {
                setString(1, it.begrunnelse)
                setBoolean(2, it.foreliggerFagligVurdering)
                setBoolean(3, it.virksomhetErNy)
                setBoolean(4, it.brukerEierVirksomheten)
                setBoolean(5, it.kanFøreTilSelvforsørget)
                setString(6, it.virksomhetNavn)
                setLong(7, it.orgNr)
                setLong(8, lagreUtviklingsperiode(it.utviklingsPerioder))
                setLong(9, lagreOppstartsperiode(it.oppstartsPerioder))
                setLong(10, vurderingerId)
                setLong(11, it.vurdertIBehandling.id)
                setString(12, it.vurdertAv.ident)
                setLocalDate(13, it.vurderingenGjelderFra)
                setLocalDate(14, it.vurderingenGjelderTil)
                setInstant(15, it.opprettetTid)
            }
        }

        connection.execute(
            """
                INSERT INTO ETABLERING_EGEN_VIRKSOMHET_GRUNNLAG (BEHANDLING_ID, VURDERINGER_ID, OPPRETTET_TID) VALUES (?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, vurderingerId)
                setInstant(3, Instant.now())
            }
        }
    }

    private fun lagreUtviklingsperiode(perioder: List<Periode>): Long {
        val utviklingPerioderQuery = """
            INSERT INTO EGEN_VIRKSOMHET_UTVIKLING_PERIODER (OPPRETTET_TID)
            VALUES (?)
        """.trimIndent()
        val utviklingPerioderId = connection.executeReturnKey(utviklingPerioderQuery)

        connection.executeBatch(
            """
                INSERT INTO EGEN_VIRKSOMHET_UTVIKLING_PERIODE (PERIODER_ID, PERIODE, OPPRETTET_TID) VALUES (?, ?::daterange, ?)
            """.trimIndent(), perioder
        ) {
            setParams {
                setLong(1, utviklingPerioderId)
                setPeriode(2, it)
                setInstant(3, Instant.now())
            }
        }

        return utviklingPerioderId
    }

    private fun lagreOppstartsperiode(perioder: List<Periode>): Long {
        val oppstartPerioderQuery = """
            INSERT INTO EGEN_VIRKSOMHET_OPPSTART_PERIODER (OPPRETTET_TID)
            VALUES (?)
        """.trimIndent()
        val oppstartPerioderId = connection.executeReturnKey(oppstartPerioderQuery)

        connection.executeBatch(
            """
                INSERT INTO EGEN_VIRKSOMHET_OPPSTART_PERIODE (PERIODER_ID, PERIODE, OPPRETTET_TID) VALUES (?, ?::daterange, ?)
            """.trimIndent(), perioder
        ) {
            setParams {
                setLong(1, oppstartPerioderId)
                setPeriode(2, it)
                setInstant(3, Instant.now())
            }
        }

        return oppstartPerioderId
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        TODO("Not yet implemented")
    }

    override fun slett(behandlingId: BehandlingId) {
        TODO("Not yet implemented")
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        TODO("Not yet implemented")
    }
}