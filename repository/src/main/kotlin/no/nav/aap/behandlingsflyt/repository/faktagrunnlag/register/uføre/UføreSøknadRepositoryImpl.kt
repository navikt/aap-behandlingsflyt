package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreSøknad
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreSøknadGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreSøknadRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory

class UføreSøknadRepositoryImpl(private val connection: DBConnection) : UføreSøknadRepository {

    companion object : Factory<UføreSøknadRepositoryImpl> {
        override fun konstruer(connection: DBConnection): UføreSøknadRepositoryImpl {
            return UføreSøknadRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): UføreSøknadGrunnlag? {
        return connection.queryFirstOrNull(
            """
            SELECT *
            FROM UFORE_SOKNAD_GRUNNLAG
            WHERE AKTIV AND BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                UføreSøknadGrunnlag(
                    behandlingId = behandlingId,
                    uføreSøknad = UføreSøknad(
                        soknadsdato = row.getLocalDate("soknadsdato"),
                        sakId = row.getLong("ufore_sak_id")
                    )
                )
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val deletedRows = connection.executeReturnUpdated(
            """
            delete from UFORE_SOKNAD_GRUNNLAG where behandling_id = ?;
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
            }
        }
    }

    override fun lagre(behandlingId: BehandlingId, uføreSøknad: UføreSøknad) {
        val eksisterendeUføreGrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeUføreGrunnlag?.uføreSøknad == uføreSøknad) return

        if (eksisterendeUføreGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        connection.execute("INSERT INTO UFORE_SOKNAD_GRUNNLAG (BEHANDLING_ID, SOKNADSDATO, UFORE_SAK_ID) VALUES (?, ?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLocalDate(2, uføreSøknad.soknadsdato)
                setLong(3, uføreSøknad.sakId)
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE UFORE_SOKNAD_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Ønsker ikke å kopiere denne fra tidligere behandlinger da dette ikke nødvendigvis er relevant
    }
}
