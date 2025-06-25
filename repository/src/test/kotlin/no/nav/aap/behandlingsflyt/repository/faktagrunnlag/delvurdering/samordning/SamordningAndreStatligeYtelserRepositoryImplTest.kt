package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.AndreStatligeYtelser
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserVurderingPeriode
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

internal class SamordningAndreStatligeYtelserRepositoryImplTest {
    private val dataSource = InitTestDatabase.freshDatabase()

    private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
    private val periodeTo = Periode(LocalDate.now(), LocalDate.now().plusYears(2))

    @Test
    fun `skal lagre ned en helt ny vurdering og hente den opp igjen`() {
        val behandling = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak(it))
        }

        // Lagre ytelse
        val vurdering = SamordningAndreStatligeYtelserVurdering(
            begrunnelse = "En fin begrunnelse",
            vurdertAv = "Lokalsaksbehandler",
            vurderingPerioder = listOf(
                SamordningAndreStatligeYtelserVurderingPeriode(
                    periode = periode,
                    beløp = 344,
                    ytelse = AndreStatligeYtelser.DAGPENGER,
                ),
                SamordningAndreStatligeYtelserVurderingPeriode(
                    periode = periodeTo,
                    beløp = 399,
                    ytelse = AndreStatligeYtelser.BARNEPENSJON,
                )
            )
        )
        dataSource.transaction {
            SamordningAndreStatligeYtelserRepositoryImpl(it).lagre(behandling.id, vurdering)
        }

        val uthentet = dataSource.transaction {
            SamordningAndreStatligeYtelserRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(uthentet).isNotNull
        assertThat(uthentet?.vurdering).usingRecursiveComparison().ignoringFields("vurdertTidspunkt").isEqualTo(vurdering)
    }

    @Test
    fun `test sletting`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val samordningAndreStatligeYtelserRepository = SamordningAndreStatligeYtelserRepositoryImpl(connection)
            samordningAndreStatligeYtelserRepository.lagre(
                behandling.id, SamordningAndreStatligeYtelserVurdering(
                    begrunnelse = "En fin begrunnelse",
                    vurdertAv = "Lokalsaksbehandler",
                    vurderingPerioder = listOf(
                        SamordningAndreStatligeYtelserVurderingPeriode(
                            periode = periode,
                            beløp = 344,
                            ytelse = AndreStatligeYtelser.DAGPENGER,
                        ),
                        SamordningAndreStatligeYtelserVurderingPeriode(
                            periode = periodeTo,
                            beløp = 399,
                            ytelse = AndreStatligeYtelser.BARNEPENSJON,
                        )
                    )
                )
            )
            samordningAndreStatligeYtelserRepository.lagre(
                behandling.id, SamordningAndreStatligeYtelserVurdering(
                    begrunnelse = "En fin begrunnelse",
                    vurdertAv = "Lokalsaksbehandler",
                    vurderingPerioder = listOf(
                        SamordningAndreStatligeYtelserVurderingPeriode(
                            periode = periode,
                            beløp = 1000,
                            ytelse = AndreStatligeYtelser.OMSTILLINGSSTØNAD,
                        ),
                        SamordningAndreStatligeYtelserVurderingPeriode(
                            periode = periodeTo,
                            beløp = 2500,
                            ytelse = AndreStatligeYtelser.TILTAKSPENGER,
                        )
                    )
                )
            )
            assertDoesNotThrow {
                samordningAndreStatligeYtelserRepository.slett(behandling.id)
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
}
