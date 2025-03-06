package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.ytelsesvurdering

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopierer
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate


class SamordningYtelseVurderingRepositoryImplTest {
    private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

    @Test
    fun `lagre og hente ut igjen`() {
        val dataSource = InitTestDatabase.dataSource
        val behandling = dataSource.transaction {
            behandling(it, sak(it))
        }

        // Lagre ytelse
        dataSource.transaction {
            SamordningYtelseVurderingRepositoryImpl(it).lagreYtelser(
                behandlingId = behandling.id,
                samordningYtelser = listOf(
                    SamordningYtelse(
                        ytelseType = Ytelse.SYKEPENGER,
                        ytelsePerioder = listOf(
                            SamordningYtelsePeriode(
                                periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3)),
                                gradering = Prosent(50),
                                kronesum = null
                            ),
                            SamordningYtelsePeriode(
                                periode = Periode(LocalDate.now().minusYears(3), LocalDate.now().minusDays(1)),
                                gradering = null,
                                kronesum = 123
                            )
                        ),
                        kilde = "XXXX",
                        saksRef = "saksref"
                    )
                )
            )
        }

        // Lagre vurdering
        val vurdering = SamordningVurdering(
            ytelseType = Ytelse.SYKEPENGER,
            begrunnelse = "En god begrunnelse",
            avslaasGrunnetLangVarighet = false,
            maksDatoEndelig = false,
            maksDato = LocalDate.now().plusYears(1),
            vurderingPerioder = listOf(
                SamordningVurderingPeriode(
                    periode = Periode(LocalDate.now().minusYears(3), LocalDate.now().minusDays(1)),
                    gradering = Prosent(40),
                    kronesum = null,
                )
            )
        )
        dataSource.transaction {
            SamordningYtelseVurderingRepositoryImpl(it).lagreVurderinger(
                behandlingId = behandling.id,
                samordningVurderinger = listOf(
                    vurdering
                )
            )
        }

        val uthentet = dataSource.transaction {
            SamordningYtelseVurderingRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }


        assertThat(uthentet?.ytelseGrunnlag?.ytelser).isEqualTo(
            listOf(
                SamordningYtelse(
                    ytelseType = Ytelse.SYKEPENGER,
                    ytelsePerioder = listOf(
                        SamordningYtelsePeriode(
                            periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3)),
                            gradering = Prosent(50),
                            kronesum = null
                        ),
                        SamordningYtelsePeriode(
                            periode = Periode(LocalDate.now().minusYears(3), LocalDate.now().minusDays(1)),
                            gradering = null,
                            kronesum = 123
                        )
                    ),
                    kilde = "XXXX",
                    saksRef = "saksref"
                )
            )
        )

        assertThat(uthentet?.vurderingGrunnlag?.vurderinger).isEqualTo(listOf(vurdering))
    }

    @Test
    fun `å lagre en vurdering før ytelse eksisterer gir feil`() {
        val dataSource = InitTestDatabase.dataSource
        val behandling = dataSource.transaction {
            behandling(it, sak(it))
        }

        // Lagre vurdering
        val vurdering = SamordningVurdering(
            ytelseType = Ytelse.SYKEPENGER,
            begrunnelse = "En god begrunnelse",
            avslaasGrunnetLangVarighet = false,
            maksDatoEndelig = false,
            maksDato = LocalDate.now().plusYears(1),
            listOf(SamordningVurderingPeriode(
                    periode = Periode(LocalDate.now().minusYears(3), LocalDate.now().minusDays(1)),
                    gradering = Prosent(40),
                    kronesum = null,
                ))
            )

        assertThrows<IllegalArgumentException> {
            dataSource.transaction {
                SamordningYtelseVurderingRepositoryImpl(it).lagreVurderinger(
                    behandlingId = behandling.id,
                    samordningVurderinger = listOf(
                        vurdering
                    )
                )
            }
        }
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

    private fun behandling(connection: DBConnection, sak: Sak): Behandling {
        return SakOgBehandlingService(
            GrunnlagKopierer(connection), SakRepositoryImpl(connection),
            BehandlingRepositoryImpl(connection)
        ).finnEllerOpprettBehandling(
            sak.saksnummer,
            listOf(Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD))
        ).behandling
    }
}