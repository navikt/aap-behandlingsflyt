package no.nav.aap.behandlingsflyt.repository.behandling.mellomlagring

import no.nav.aap.behandlingsflyt.behandling.mellomlagring.MellomlagretVurdering
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MellomlagretVurderingRepositoryImplTest {

    companion object {
        private val dataSource = InitTestDatabase.freshDatabase()
    }

    @Test
    fun `lagrer ned mellomlagret vurdering og henter ut`() {
        dataSource.transaction { connection ->
            val repository = MellomlagretVurderingRepositoryImpl(connection)
            val behandlingId = opprettBehandling(connection)
            val vurdering = opprettMellomlagretVurdering(behandlingId, "1234")
            repository.lagre(vurdering)

            val mellomlagretVurdering = repository.hentHvisEksisterer(behandlingId, "1234")

            assertThat(mellomlagretVurdering).isNotNull()
            assertThat(mellomlagretVurdering?.behandlingId).isEqualTo(behandlingId)
            assertThat(mellomlagretVurdering?.vurdertDato).isEqualTo(vurdering.vurdertDato)
            assertThat(mellomlagretVurdering?.vurdertAv).isEqualTo(vurdering.vurdertAv)
            assertThat(mellomlagretVurdering?.avklaringsbehovKode).isEqualTo(vurdering.avklaringsbehovKode)
            assertThat(DefaultJsonMapper.fromJson<FakeDataObjektForMellomlager>(mellomlagretVurdering!!.data)).isEqualTo(DefaultJsonMapper.fromJson<FakeDataObjektForMellomlager>(vurdering.data))
        }
    }

    @Test
    fun `skal overskrive forrige vurdering dersom det kommer inn ny`() {
        dataSource.transaction { connection ->
            val repository = MellomlagretVurderingRepositoryImpl(connection)
            val behandlingId = opprettBehandling(connection)
            val vurdering = opprettMellomlagretVurdering(behandlingId, "1234")
            repository.lagre(vurdering)

            val data = FakeDataObjektForMellomlager("tull", true, true, 0)
            val oppdatertVurdering = opprettMellomlagretVurdering(behandlingId, "1234", bruker = "bruker2", data = data)
            repository.lagre(oppdatertVurdering)

            val mellomlagretVurdering = repository.hentHvisEksisterer(behandlingId, "1234")
            assertThat(mellomlagretVurdering).isNotNull()
            assertThat(mellomlagretVurdering?.behandlingId).isEqualTo(behandlingId)
            assertThat(mellomlagretVurdering?.vurdertDato).isEqualTo(oppdatertVurdering.vurdertDato)
            assertThat(mellomlagretVurdering?.vurdertAv).isEqualTo(oppdatertVurdering.vurdertAv)
            assertThat(mellomlagretVurdering?.avklaringsbehovKode).isEqualTo(oppdatertVurdering.avklaringsbehovKode)
            assertThat(DefaultJsonMapper.fromJson<FakeDataObjektForMellomlager>(mellomlagretVurdering!!.data)).isEqualTo(data)


        }
    }

    @Test
    fun `skal hente ut vurderinger på en behandling`() {
        dataSource.transaction { connection ->
            val repository = MellomlagretVurderingRepositoryImpl(connection)
            val behandlingId = opprettBehandling(connection)
            val behandlingId2 = opprettBehandling(connection)
            val vurdering1 = opprettMellomlagretVurdering(behandlingId, "1", bruker = "b1")
            val vurdering2 = opprettMellomlagretVurdering(behandlingId, "2", bruker = "b1")
            val vurdering3 = opprettMellomlagretVurdering(behandlingId, "3", bruker = "b1")
            val vurdering4 = opprettMellomlagretVurdering(behandlingId2, "2", bruker = "b2")

            repository.lagre(vurdering1)
            repository.lagre(vurdering2)
            repository.lagre(vurdering3)

            // Gjelder en annen behandling
            repository.lagre(vurdering4)

            val mellomlagretVurdering = repository.hentHvisEksisterer(behandlingId, vurdering2.avklaringsbehovKode)
            assertThat(mellomlagretVurdering).isNotNull()
            assertThat(mellomlagretVurdering?.avklaringsbehovKode).isEqualTo(vurdering2.avklaringsbehovKode)
            assertThat(mellomlagretVurdering?.vurdertAv).isEqualTo(vurdering2.vurdertAv)
            assertThat(mellomlagretVurdering?.vurdertDato).isEqualTo(vurdering2.vurdertDato)
            assertThat(mellomlagretVurdering?.data).isNotNull
        }
    }


    private fun opprettBehandling(connection: DBConnection): BehandlingId {
        val person = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("ident", true)))
        val sakId = SakRepositoryImpl(connection).finnEllerOpprett(
            person,
            Periode(LocalDate.now(), LocalDate.now().plusDays(5))
        ).id
        return BehandlingRepositoryImpl(connection).opprettBehandling(
            sakId,
            listOf(),
            TypeBehandling.Førstegangsbehandling,
            null
        ).id
    }

    private fun opprettMellomlagretVurdering(
        behandlingId: BehandlingId,
        avklaringsbehovKode: String,
        bruker: String = "bruker123",
        data: FakeDataObjektForMellomlager = FakeDataObjektForMellomlager()
    ): MellomlagretVurdering = MellomlagretVurdering(
        behandlingId = behandlingId,
        avklaringsbehovKode = avklaringsbehovKode,
        data = DefaultJsonMapper.toJson(data),
        vurdertAv = bruker,
        vurdertDato = LocalDateTime.now().withNano(0)
    )

    private data class FakeDataObjektForMellomlager(val beskrivelse: String = "En tekstlig beskrivelse", val harOppfyltA: Boolean = true, val harOppfyltB: Boolean = false, val enTallVerdi: Int = 1324)


}