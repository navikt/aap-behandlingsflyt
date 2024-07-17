package no.nav.aap.behandlingsflyt.server.prosessering

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.hendelse.avløp.VilkårsResultatHendelseDTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkGateway
import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkHendelseDTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.VilkårDTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.VilkårsPeriodeDTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.VilkårsResultatDTO
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.json.DefaultJsonMapper
import no.nav.aap.motor.JobbInput
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident
import no.nav.aap.verdityper.sakogbehandling.Status
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class StatistikkJobbUtførerTest {
    companion object {
        private val fakes = Fakes()

        @JvmStatic
        @AfterAll
        fun afterAll() {
            fakes.close()
        }
    }

    @Test
    fun `statistikk-jobb avgir vilkårs-resultat korrekt`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val vilkårsResultatRepository = VilkårsresultatRepository(connection = connection)
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val sakService = SakService(connection)
            val utfører =
                StatistikkJobbUtfører(StatistikkGateway(), vilkårsResultatRepository, behandlingRepository, sakService)

            val identGateway = mockk<IdentGateway>()
            every { identGateway.hentAlleIdenterForPerson(any()) } returns listOf(
                Ident(
                    identifikator = "123",
                    aktivIdent = true
                )
            )

            val sak = PersonOgSakService(connection, identGateway).finnEllerOpprett(
                Ident(
                    identifikator = "123",
                    aktivIdent = true
                ), periode = Periode(LocalDate.now().minusDays(10), LocalDate.now().plusDays(1))
            )

            val id = behandlingRepository.opprettBehandling(
                sak.id,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                årsaker = listOf()
            ).id

            val vilkårsresultat = Vilkårsresultat(
                vilkår = listOf(
                    Vilkår(
                        type = Vilkårtype.MEDLEMSKAP, vilkårsperioder = setOf(
                            Vilkårsperiode(
                                Periode(
                                    fom = LocalDate.now().minusDays(1),
                                    tom = LocalDate.now().plusDays(1)
                                ),
                                Utfall.OPPFYLT,
                                false,
                                "ignorert",
                                null,
                                null,
                                null,
                                "123"
                            )
                        )
                    )
                )
            )
            vilkårsResultatRepository.lagre(
                id, vilkårsresultat
            )

            val payload = VilkårsResultatHendelseDTO(behandlingId = id)
            val hendelse2 = DefaultJsonMapper.toJson(payload)

            utfører.utfør(
                JobbInput(StatistikkJobbUtfører).medPayload(hendelse2)
                    .medParameter("statistikk-type", StatistikkType.AvsluttetBehandling.toString())
            )

            assertThat(fakes.mottatteVilkårsResult).isNotEmpty()
            assertThat(fakes.mottatteVilkårsResult.first()).isEqualTo(
                VilkårsResultatDTO(
                    saksnummer = sak.saksnummer,
                    typeBehandling = TypeBehandling.Førstegangsbehandling,
                    vilkår = listOf(
                        VilkårDTO(
                            vilkårType = Vilkårtype.MEDLEMSKAP,
                            perioder = listOf(
                                VilkårsPeriodeDTO(
                                    fraDato = LocalDate.now().minusDays(1),
                                    tilDato = LocalDate.now().plusDays(1),
                                    utfall = Utfall.OPPFYLT,
                                    manuellVurdering = false,
                                    avslagsårsak = null,
                                    innvilgelsesårsak = null,
                                )
                            )
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `prosesserings-kall avgir statistikk korrekt`() {
        // Blir ikke kalt (ennå), så derfor bare mock (vil fjerne etter refaktorering)
        val vilkårsResultatRepository = mockk<VilkårsresultatRepository>()
        val behandlingRepository = mockk<BehandlingRepository>()
        val sakSerice = mockk<SakService>()
        val utfører =
            StatistikkJobbUtfører(StatistikkGateway(), vilkårsResultatRepository, behandlingRepository, sakSerice)

        val payload = BehandlingFlytStoppetHendelse(
            personident = "123",
            status = Status.UTREDES,
            behandlingType = TypeBehandling.Klage,
            referanse = BehandlingReferanse("123"),
            saksnummer = Saksnummer("456"),
            opprettetTidspunkt = LocalDateTime.now(),
            avklaringsbehov = listOf()
        )

        val hendelse = DefaultJsonMapper.toJson(payload)

        utfører.utfør(
            JobbInput(StatistikkJobbUtfører).medPayload(hendelse)
                .medParameter("statistikk-type", StatistikkType.BehandlingStoppet.toString())
        )

        assertThat(fakes.statistikkHendelser).isNotEmpty()
        assertThat(fakes.statistikkHendelser.size).isEqualTo(1)
        assertThat(fakes.statistikkHendelser.first()).isEqualTo(
            StatistikkHendelseDTO(
                saksnummer = "456",
                status = Status.UTREDES,
                behandlingType = TypeBehandling.Klage,
            )
        )
    }
}