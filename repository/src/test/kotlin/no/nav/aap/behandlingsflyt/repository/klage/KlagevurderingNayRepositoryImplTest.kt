package no.nav.aap.behandlingsflyt.repository.klage

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.KlageInnstilling
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagevurderingNay
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage.KlagebehandlingNayRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
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

class KlagevurderingNayRepositoryImplTest {
    private val dataSource = InitTestDatabase.freshDatabase()

    @Test
    fun `Lagrer og henter klagevurdering nay`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            finnEllerOpprettBehandling(connection, sak)
            val klageBehandling = finnEllerOpprettBehandling(connection, sak, ÅrsakTilBehandling.MOTATT_KLAGE)

            val klagebehandlingNayRepository = KlagebehandlingNayRepositoryImpl(connection)
            val vurdering = KlagevurderingNay(
                begrunnelse = "begrunnelse",
                notat = null,
                innstilling = KlageInnstilling.OPPRETTHOLD,
                vilkårSomOmgjøres = emptyList(),
                vilkårSomOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5),
                vurdertAv = "ident"
            )

            klagebehandlingNayRepository.lagre(klageBehandling.id, vurdering)
            val grunnlag = klagebehandlingNayRepository.hentHvisEksisterer(klageBehandling.id)!!
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