package no.nav.aap.behandlingsflyt.repository.klage

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.KlageInnstilling
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagevurderingKontor
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage.KlagebehandlingKontorRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import java.time.LocalDate

class KlagevurderingKontorRepositoryImplTest {
    private val dataSource = InitTestDatabase.freshDatabase()

    @Test
    fun `Lagrer og henter klagevurdering kontor`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            finnEllerOpprettBehandling(connection, sak)
            val klageBehandling = finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.MOTATT_KLAGE)

            val klagebehandlingKontorRepository = KlagebehandlingKontorRepositoryImpl(connection)
            val vurdering = KlagevurderingKontor(
                begrunnelse = "begrunnelse",
                notat = null,
                innstilling = KlageInnstilling.OPPRETTHOLD,
                vilkårSomOmgjøres = emptyList(),
                vilkårSomOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5),
                vurdertAv = "ident"
            )

            klagebehandlingKontorRepository.lagre(klageBehandling.id, vurdering)
            val grunnlag = klagebehandlingKontorRepository.hentHvisEksisterer(klageBehandling.id)!!
            assertThat(grunnlag.vurdering.begrunnelse).isEqualTo("begrunnelse")
            assertNull(grunnlag.vurdering.notat)
            assertThat(grunnlag.vurdering.innstilling).isEqualTo(KlageInnstilling.OPPRETTHOLD)
            assertThat(grunnlag.vurdering.vilkårSomOmgjøres).isEmpty()
            assertThat(grunnlag.vurdering.vilkårSomOpprettholdes).containsExactly(Hjemmel.FOLKETRYGDLOVEN_11_5)
            assertThat(grunnlag.vurdering.vurdertAv).isEqualTo("ident")
            assertNotNull(grunnlag.vurdering.opprettet)
        }
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }
}