package no.nav.aap.behandlingsflyt.server.prosessering

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.hendelse.avløp.AvsluttetBehandlingHendelseDTO
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.hendelse.statistikk.AvsluttetBehandlingDTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.BeregningsgrunnlagDTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.Grunnlag11_19DTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkGateway
import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkHendelseDTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.TilkjentYtelseDTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.VilkårDTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.VilkårsPeriodeDTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.VilkårsResultatDTO
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.json.DefaultJsonMapper
import no.nav.aap.motor.JobbInput
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident
import no.nav.aap.verdityper.sakogbehandling.SakId
import no.nav.aap.verdityper.sakogbehandling.Status
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

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
    fun `statistikk-jobb avgir avsluttet behandling-data korrekt`() {
        val (behandling, sak) = InitTestDatabase.dataSource.transaction { connection ->
            val vilkårsResultatRepository = VilkårsresultatRepository(connection = connection)
            val behandlingRepository = BehandlingRepositoryImpl(connection)

            val beregningsgrunnlagRepository = BeregningsgrunnlagRepository(connection)

            val ident = Ident(
                identifikator = "123",
                aktivIdent = true
            )
            val identGateway = object : IdentGateway {
                override fun hentAlleIdenterForPerson(ident: Ident): List<Ident> {
                    return listOf(ident)
                }
            }

            val sak = PersonOgSakService(connection, identGateway).finnEllerOpprett(
                ident, periode = Periode(LocalDate.now().minusDays(10), LocalDate.now().plusDays(1))
            )

            val opprettetBehandling = behandlingRepository.opprettBehandling(
                sak.id,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                årsaker = listOf()
            )
            beregningsgrunnlagRepository.lagre(
                behandlingId = opprettetBehandling.id,
                Grunnlag11_19(
                    grunnlaget = GUnit(7),
                    er6GBegrenset = true, erGjennomsnitt = false,
                    inntekter = listOf()
                )
            )

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
                opprettetBehandling.id, vilkårsresultat
            )

            Pair(opprettetBehandling, sak)
        }

        val payload = AvsluttetBehandlingHendelseDTO(behandlingId = behandling.id)
        val hendelse2 = DefaultJsonMapper.toJson(payload)

        // Act

        InitTestDatabase.dataSource.transaction { connection ->
            val sakService = SakService(connection)
            val vilkårsResultatRepository = VilkårsresultatRepository(connection = connection)
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val beregningsgrunnlagRepository = BeregningsgrunnlagRepository(connection)

            StatistikkJobbUtfører(
                StatistikkGateway(),
                vilkårsResultatRepository,
                behandlingRepository,
                sakService,
                TilkjentYtelseRepository(connection),
                beregningsgrunnlagRepository
            ).utfør(
                JobbInput(StatistikkJobbUtfører).medPayload(hendelse2)
                    .medParameter("statistikk-type", StatistikkType.AvsluttetBehandling.toString())
            )
        }

        // Assert

        assertThat(fakes.mottatteVilkårsResult).isNotEmpty()
        assertThat(fakes.mottatteVilkårsResult.first()).isEqualTo(
            AvsluttetBehandlingDTO(
                behandlingsReferanse = behandling.referanse,
                saksnummer = sak.saksnummer,
                tilkjentYtelse = TilkjentYtelseDTO(perioder = listOf()),
                beregningsGrunnlag = BeregningsgrunnlagDTO(
                    grunnlag = 7.0,
                    er6GBegrenset = false,
                    grunnlag11_19dto = Grunnlag11_19DTO(inntekter = mapOf())
                ),
                vilkårsResultat =
                VilkårsResultatDTO(
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
        )
    }

    @Test
    fun `prosesserings-kall avgir statistikk korrekt`() {
        // Blir ikke kalt i denne metoden, så derfor bare mock
        val vilkårsResultatRepository = mockk<VilkårsresultatRepository>()
        val behandlingRepository = mockk<BehandlingRepository>()
        val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
        val beregningsgrunnlagRepository = mockk<BeregningsgrunnlagRepository>()

        // Mock her siden dette er eneste eksterne kall
        val sakService = mockk<SakService>()
        every { sakService.hent(SakId(123)) } returns Sak(
            id = SakId(123),
            person = Person(0, identifikator = UUID.randomUUID(), identer = listOf(Ident("1234", aktivIdent = true))),
            rettighetsperiode = Periode(fom = LocalDate.now(), tom = LocalDate.now().plusDays(1)),
            saksnummer = Saksnummer("456")
        )

        val utfører =
            StatistikkJobbUtfører(
                StatistikkGateway(),
                vilkårsResultatRepository,
                behandlingRepository,
                sakService,
                tilkjentYtelseRepository,
                beregningsgrunnlagRepository,
            )

        val payload = BehandlingFlytStoppetHendelse(
            saksnummer = Saksnummer("456"),
            personIdent = "123",
            status = Status.UTREDES,
            behandlingType = TypeBehandling.Klage,
            referanse = BehandlingReferanse("123"),
            opprettetTidspunkt = LocalDateTime.now(),
            avklaringsbehov = listOf()
        )

        val hendelse = DefaultJsonMapper.toJson(payload)

        // Act
        utfører.utfør(
            JobbInput(StatistikkJobbUtfører).medPayload(hendelse)
                .medParameter("statistikk-type", StatistikkType.BehandlingStoppet.toString())
        )

        // Assert
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