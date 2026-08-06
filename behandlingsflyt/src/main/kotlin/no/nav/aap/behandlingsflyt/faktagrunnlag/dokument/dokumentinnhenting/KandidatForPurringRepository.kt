package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.Repository
import no.nav.aap.komponenter.repository.RepositoryFactory
import java.time.LocalDate
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status as AvklaringsbehovStatus

interface KandidatForPurringRepository : Repository {
    fun finnKandidaterForPurring(dato: LocalDate = LocalDate.now()): List<BehandlingReferanse>
}

class KandidatForPurringRepositoryImpl(
    private val connection: DBConnection
) : KandidatForPurringRepository {
    /**
     * En kandidat for purring er en behandling som:
     * - er åpen
     * - har en OPPRETTET-endring på BESTILL_LEGEERKLÆRING-avklaringsbehovet som er nøyaktig tre uker og en dag gammel
     * - ikke har vurderingsbehov [no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.MOTTATT_DIALOGMELDING]
     *   eller [no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.MOTTATT_LEGEERKLÆRING] som er nyere enn tre uker og en dag gammelt.
     */
    override fun finnKandidaterForPurring(dato: LocalDate): List<BehandlingReferanse> {
        val treUkerOgEnDagSiden = dato.minusWeeks(3).minusDays(1)
        val query = """
            SELECT b.referanse
            FROM BEHANDLING b
             JOIN AVKLARINGSBEHOV a ON a.behandling_id = b.id
            WHERE b.STATUS in ('${Status.UTREDES.name}', '${Status.OPPRETTET.name}')
            AND a.definisjon = '${Definisjon.BESTILL_LEGEERKLÆRING.kode.name}'
            AND EXISTS (
                SELECT 1
                FROM AVKLARINGSBEHOV_ENDRING ae
                WHERE ae.AVKLARINGSBEHOV_ID = a.id
                AND ae.status = '${AvklaringsbehovStatus.OPPRETTET.name}'
                AND ae.opprettet_tid::date = ?
            )
             AND NOT EXISTS (
                SELECT 1
                FROM VURDERINGSBEHOV v
                WHERE v.behandling_id = b.id
                AND v.aarsak IN ('${Vurderingsbehov.MOTTATT_DIALOGMELDING.name}', '${Vurderingsbehov.MOTTATT_LEGEERKLÆRING.name}')
                AND v.opprettet_tid::date >= ?
            )
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLocalDate(1, treUkerOgEnDagSiden)
                setLocalDate(2, treUkerOgEnDagSiden)
            }
            setRowMapper { row ->
                val behandlingReferanse = row.getUUID("referanse")
                BehandlingReferanse(behandlingReferanse)
            }
        }
    }

    companion object : RepositoryFactory<KandidatForPurringRepository> {
        override fun konstruer(connection: DBConnection) = KandidatForPurringRepositoryImpl(connection)
    }
}