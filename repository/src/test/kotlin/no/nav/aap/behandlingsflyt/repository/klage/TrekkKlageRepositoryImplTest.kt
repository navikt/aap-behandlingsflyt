package no.nav.aap.behandlingsflyt.repository.klage

import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageVurdering
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageÅrsak
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage.TrekkKlageRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

internal class TrekkKlageRepositoryImplTest {
    private val dataSource = InitTestDatabase.freshDatabase()
    
    @Test
    fun `Lagrer og henter trukket klage`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            finnEllerOpprettBehandling(connection, sak)
            val klageBehandling = finnEllerOpprettBehandling(connection, sak, ÅrsakTilBehandling.MOTATT_KLAGE)
            
            val trekkKlageRepository = TrekkKlageRepositoryImpl(connection)
            val vurdering = TrekkKlageVurdering(
                begrunnelse = "En begrunnelse",
                skalTrekkes = true,
                hvorforTrekkes = TrekkKlageÅrsak.TRUKKET_AV_BRUKER,
                vurdert = LocalDateTime.now().toInstant(ZoneOffset.UTC),
                vurdertAv = Bruker(ident = "12345"),
            )
            
            trekkKlageRepository.lagreTrekkKlageVurdering(behandlingId = klageBehandling.id, vurdering = vurdering)
            val grunnlag = trekkKlageRepository.hentTrekkKlageGrunnlag(klageBehandling.id)!!
            assertEqualVurdering(grunnlag.vurdering, vurdering)
        }
    }

    @Test
    fun `kan lagre flere vurderinger på samme klage og hente ut nyeste vurdering som del av grunnlaget`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            finnEllerOpprettBehandling(connection, sak)
            val klageBehandling = finnEllerOpprettBehandling(connection, sak, ÅrsakTilBehandling.MOTATT_KLAGE)

            val trekkKlageRepository = TrekkKlageRepositoryImpl(connection)
            val vurdering1 = TrekkKlageVurdering(
                begrunnelse = "En begrunnelse",
                skalTrekkes = false,
                hvorforTrekkes = null,
                vurdert = LocalDateTime.now().toInstant(ZoneOffset.UTC),
                vurdertAv = Bruker(ident = "12345"),
            )

            val vurdering2 = TrekkKlageVurdering(
                begrunnelse = "En ny begrunnelse",
                skalTrekkes = true,
                hvorforTrekkes = TrekkKlageÅrsak.TRUKKET_AV_BRUKER,
                vurdert = LocalDateTime.now().toInstant(ZoneOffset.UTC),
                vurdertAv = Bruker(ident = "54321"),
            )

            trekkKlageRepository.lagreTrekkKlageVurdering(behandlingId = klageBehandling.id, vurdering = vurdering1)
            trekkKlageRepository.lagreTrekkKlageVurdering(behandlingId = klageBehandling.id, vurdering = vurdering2)
            val grunnlag = trekkKlageRepository.hentTrekkKlageGrunnlag(klageBehandling.id)!!
            assertEqualVurdering(grunnlag.vurdering, vurdering2)
        }
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }

    private fun assertEqualVurdering(vurdering1: TrekkKlageVurdering, vurdering2: TrekkKlageVurdering) {
        assertThat(vurdering1.vurdertAv.ident).isEqualTo(vurdering2.vurdertAv.ident)
        assertThat(vurdering1.skalTrekkes).isEqualTo(vurdering2.skalTrekkes)
        assertThat(vurdering1.hvorforTrekkes).isEqualTo(vurdering2.hvorforTrekkes)
        assertThat(vurdering1.begrunnelse).isEqualTo(vurdering2.begrunnelse)
        assertThat(vurdering1.hvorforTrekkes).isEqualTo(vurdering2.hvorforTrekkes)
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }
}