package no.nav.aap.behandlingsflyt.server.prosessering

import io.mockk.checkUnnecessaryStub
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.hendelse.avløp.AvsluttetBehandlingHendelseDTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkGateway
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.AvklaringsbehovHendelseDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.DefinisjonDTO
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.EndringDTO
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.JobbInput
import no.nav.aap.statistikk.api_kontrakt.AvklaringsbehovHendelse
import no.nav.aap.statistikk.api_kontrakt.AvsluttetBehandlingDTO
import no.nav.aap.statistikk.api_kontrakt.BehovType
import no.nav.aap.statistikk.api_kontrakt.BeregningsgrunnlagDTO
import no.nav.aap.statistikk.api_kontrakt.Endring
import no.nav.aap.statistikk.api_kontrakt.EndringStatus
import no.nav.aap.statistikk.api_kontrakt.Grunnlag11_19DTO
import no.nav.aap.statistikk.api_kontrakt.StoppetBehandling
import no.nav.aap.statistikk.api_kontrakt.TilkjentYtelseDTO
import no.nav.aap.statistikk.api_kontrakt.VilkårDTO
import no.nav.aap.statistikk.api_kontrakt.VilkårsPeriodeDTO
import no.nav.aap.statistikk.api_kontrakt.VilkårsResultatDTO
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.sakogbehandling.Ident
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
                    erGjennomsnitt = false,
                    gjennomsnittligInntektIG = GUnit(0),
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
        assertThat(fakes.mottatteVilkårsResult.first().toString()).isEqualTo(
            AvsluttetBehandlingDTO(
                behandlingsReferanse = behandling.referanse.referanse,
                saksnummer = sak.saksnummer.toString(),
                tilkjentYtelse = TilkjentYtelseDTO(perioder = listOf()),
                beregningsGrunnlag = BeregningsgrunnlagDTO(
                    grunnlag11_19dto = Grunnlag11_19DTO(
                        inntekter = mapOf(),
                        grunnlaget = 7.0,
                        er6GBegrenset = false,
                        erGjennomsnitt = false,
                    )
                ),
                vilkårsResultat =
                VilkårsResultatDTO(
                    typeBehandling = TypeBehandling.Førstegangsbehandling.toString(),
                    vilkår = listOf(
                        VilkårDTO(
                            vilkårType = no.nav.aap.statistikk.api_kontrakt.Vilkårtype.valueOf(Vilkårtype.MEDLEMSKAP.toString()),
                            perioder = listOf(
                                VilkårsPeriodeDTO(
                                    fraDato = LocalDate.now().minusDays(1),
                                    tilDato = LocalDate.now().plusDays(1),
                                    utfall = no.nav.aap.statistikk.api_kontrakt.Utfall.valueOf(Utfall.OPPFYLT.toString()),
                                    manuellVurdering = false,
                                    avslagsårsak = null,
                                    innvilgelsesårsak = "null",
                                )
                            )
                        )
                    )
                )
            ).toString()
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

        val utfører =
            StatistikkJobbUtfører(
                StatistikkGateway(),
                vilkårsResultatRepository,
                behandlingRepository,
                sakService,
                tilkjentYtelseRepository,
                beregningsgrunnlagRepository,
            )

        val avklaringsbehov = listOf(
            AvklaringsbehovHendelseDto(
                definisjon = DefinisjonDTO(
                    type = "xxx",
                    behovType = Definisjon.BehovType.MANUELT_PÅKREVD, løsesISteg = StegType.FATTE_VEDTAK
                ),
                status = no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER,
                endringer = listOf(
                    EndringDTO(
                        status = no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.SENDT_TILBAKE_FRA_BESLUTTER,
                        endretAv = "kanskje_meg"
                    )
                )
            )
        )
        val referanse = BehandlingReferanse()
        val fødselsNummer = Ident("xxx").toString()
        val payload = BehandlingFlytStoppetHendelse(
            saksnummer = Saksnummer("456"),
            personIdent = fødselsNummer,
            status = Status.UTREDES,
            behandlingType = TypeBehandling.Klage,
            referanse = referanse,
            opprettetTidspunkt = LocalDateTime.now(),
            avklaringsbehov = avklaringsbehov,
            hendelsesTidspunkt = LocalDateTime.now(),
            versjon = ApplikasjonsVersjon.versjon
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
            StoppetBehandling(
                saksnummer = "456",
                behandlingReferanse = referanse.referanse,
                status = Status.UTREDES.toString(),
                behandlingType = no.nav.aap.statistikk.api_kontrakt.TypeBehandling.valueOf(TypeBehandling.Klage.toString()),
                ident = fødselsNummer,
                avklaringsbehov = avklaringsbehov.map { avklaringsbehovHendelseDto ->
                    AvklaringsbehovHendelse(
                        definisjon = no.nav.aap.statistikk.api_kontrakt.Definisjon(
                            type = avklaringsbehovHendelseDto.definisjon.type,
                            behovType = BehovType.valueOf(avklaringsbehovHendelseDto.definisjon.behovType.toString()),
                            løsesISteg = avklaringsbehovHendelseDto.definisjon.løsesISteg.toString()
                        ),
                        status = EndringStatus.valueOf(avklaringsbehovHendelseDto.status.toString()),
                        endringer = avklaringsbehovHendelseDto.endringer.map { endring ->
                            Endring(
                                status = EndringStatus.valueOf(endring.status.toString()),
                                tidsstempel = endring.tidsstempel,
                                frist = endring.frist,
                                endretAv = endring.endretAv
                            )
                        }
                    )
                },
                behandlingOpprettetTidspunkt = payload.opprettetTidspunkt,
                versjon = ApplikasjonsVersjon.versjon
            )
        )

        checkUnnecessaryStub(
            sakService,
            beregningsgrunnlagRepository,
            tilkjentYtelseRepository,
            behandlingRepository,
            vilkårsResultatRepository
        )
    }
}