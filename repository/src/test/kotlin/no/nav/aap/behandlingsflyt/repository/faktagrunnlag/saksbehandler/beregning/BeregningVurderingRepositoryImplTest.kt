package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.YrkesskadeBeløpVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class BeregningVurderingRepositoryImplTest {
    private val dataSource = InitTestDatabase.freshDatabase()

    @Test
    fun `lagre, hent ut igjen, og slett`() {
        val tidspunktVurdering = BeregningstidspunktVurdering(
            begrunnelse = "Dette er en begrunnelse",
            nedsattArbeidsevneDato = LocalDate.now(),
            ytterligereNedsattBegrunnelse = "Dette er en ytterligere begrunnelse",
            ytterligereNedsattArbeidsevneDato = LocalDate.now().plusDays(30),
            vurdertAv = "saksbehandler"
        )

        val yrkesskadeVurderinger = listOf(
            YrkesskadeBeløpVurdering(
                antattÅrligInntekt = Beløp(BigDecimal("450000")),
                referanse = "Referanse 1",
                begrunnelse = "Begrunnelse for yrkesskade 1",
                vurdertAv = "saksbehandler"
            ),
            YrkesskadeBeløpVurdering(
                antattÅrligInntekt = Beløp(BigDecimal("500000")),
                referanse = "Referanse 2",
                begrunnelse = "Begrunnelse for yrkesskade 2",
                vurdertAv = "saksbehandler"
            )
        )

        val sak = dataSource.transaction { sak(it) }
        val behandling = dataSource.transaction { connection ->
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val beregningVurderingRepository = BeregningVurderingRepositoryImpl(connection)

            // Lagre tidspunktVurdering
            beregningVurderingRepository.lagre(behandling.id, tidspunktVurdering)

            // Lagre yrkesskadeVurderinger
            beregningVurderingRepository.lagre(behandling.id, yrkesskadeVurderinger)

            behandling
        }

        dataSource.transaction { connection ->
            val beregningVurderingRepository = BeregningVurderingRepositoryImpl(connection)

            // Hent og verifiser
            val beregningGrunnlag = beregningVurderingRepository.hent(behandling.id)

            // Verifiser tidspunktVurdering
            assertThat(beregningGrunnlag.tidspunktVurdering).isNotNull
                .extracting(
                    { it!!.begrunnelse },
                    { it!!.nedsattArbeidsevneDato },
                    { it!!.ytterligereNedsattBegrunnelse },
                    { it!!.ytterligereNedsattArbeidsevneDato }
                )
                .containsExactly(
                    tidspunktVurdering.begrunnelse,
                    tidspunktVurdering.nedsattArbeidsevneDato,
                    tidspunktVurdering.ytterligereNedsattBegrunnelse,
                    tidspunktVurdering.ytterligereNedsattArbeidsevneDato
                )

            // Verifiser yrkesskadeVurderinger
            assertThat(beregningGrunnlag.yrkesskadeBeløpVurdering?.vurderinger?.size).isEqualTo(yrkesskadeVurderinger.size)

            // Verifiser at alle vurderinger finnes, uavhengig av rekkefølge
            val hentedeVurderinger = beregningGrunnlag.yrkesskadeBeløpVurdering?.vurderinger
            assertThat(hentedeVurderinger)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("vurdertTidspunkt")
                .containsExactlyInAnyOrderElementsOf(yrkesskadeVurderinger)

            val behandling2 = finnEllerOpprettBehandling(connection, sak)
            assertThat(behandling2.id).isNotEqualTo(behandling.id)
            beregningVurderingRepository.kopier(behandling.id, behandling2.id)

            assertThat(beregningVurderingRepository.hentHvisEksisterer(behandling2.id)).isNotNull

            // Slett og verifiser at det er slettet
            beregningVurderingRepository.slett(behandling.id)
            assertThat(beregningVurderingRepository.hentHvisEksisterer(behandling.id)).isNull()
        }
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(
            ident(),
            periode
        )
    }
}
