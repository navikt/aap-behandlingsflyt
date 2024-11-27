package no.nav.aap.behandlingsflyt.server.prosessering

import io.mockk.checkUnnecessaryStub
import io.mockk.every
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
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkGateway
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.AvklaringsbehovHendelseDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.DefinisjonDTO
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.EndringDTO
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status.UTREDES
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.AvsluttetBehandlingDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.BeregningsgrunnlagDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Grunnlag11_19DTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StoppetBehandling
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.TilkjentYtelseDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.VilkårDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.VilkårsPeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.VilkårsResultatDTO
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.motor.JobbInput
import no.nav.aap.pip.PipRepository
import no.nav.aap.verdityper.dokument.Kanal
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.Ident
import no.nav.aap.verdityper.sakogbehandling.SakId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Fakes
class StatistikkJobbUtførerTest {

    @Test
    fun `mottatt tidspunkt er korrekt når revurdering`(hendelser: List<StoppetBehandling>) {
        var opprettetTidspunkt: LocalDateTime? = null
        val (behandling, sak, ident) = InitTestDatabase.dataSource.transaction { connection ->
            val behandlingRepository = BehandlingRepositoryImpl(connection)

            val ident = Ident(
                identifikator = "456",
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
                årsaker = listOf(),
                forrigeBehandlingId = null
            )

            val revurdering = behandlingRepository.opprettBehandling(
                sak.id,
                typeBehandling = TypeBehandling.Revurdering,
                årsaker = listOf(),
                forrigeBehandlingId = opprettetBehandling.id
            )

            opprettetTidspunkt = revurdering.opprettetTidspunkt

            MottattDokumentRepository(connection).lagre(
                MottattDokument(
                    referanse = InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "xxx"),
                    sakId = sak.id,
                    behandlingId = opprettetBehandling.id,
                    mottattTidspunkt = LocalDateTime.now().minusDays(23),
                    type = InnsendingType.SØKNAD,
                    kanal = Kanal.PAPIR,
                    strukturertDokument = null
                )
            )

            Triple(revurdering, sak, ident)
        }

        val hendelseTidspunkt = LocalDateTime.now()
        val payload = BehandlingFlytStoppetHendelse(
            personIdent = ident.identifikator,
            saksnummer = sak.saksnummer,
            referanse = behandling.referanse,
            behandlingType = behandling.typeBehandling(),
            status = behandling.status(),
            avklaringsbehov = listOf(),
            opprettetTidspunkt = opprettetTidspunkt!!,
            hendelsesTidspunkt = hendelseTidspunkt,
            versjon = "123"
        )

        val hendelse2 = DefaultJsonMapper.toJson(payload)

        // Act

        InitTestDatabase.dataSource.transaction { connection ->
            val sakService = SakService(SakRepositoryImpl(connection))
            val vilkårsResultatRepository = VilkårsresultatRepository(connection = connection)
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val beregningsgrunnlagRepository = BeregningsgrunnlagRepository(connection)

            StatistikkJobbUtfører(
                StatistikkGateway(),
                vilkårsResultatRepository,
                behandlingRepository,
                sakService,
                TilkjentYtelseRepository(connection),
                beregningsgrunnlagRepository,
                dokumentRepository = MottattDokumentRepository(connection),
                pipRepository = PipRepository(connection)
            ).utfør(
                JobbInput(StatistikkJobbUtfører).medPayload(hendelse2)
            )
        }

        // Assert

        assertThat(hendelser).isNotEmpty()
        assertThat(hendelser.size).isEqualTo(1)
        assertThat(hendelser.first().mottattTid.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(
            opprettetTidspunkt!!.truncatedTo(
                ChronoUnit.SECONDS
            )
        )
    }

    @Test
    fun `statistikk-jobb avgir avsluttet behandling-data korrekt`(hendelser: List<StoppetBehandling>) {
        val (behandling, sak, ident) = InitTestDatabase.dataSource.transaction { connection ->
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
                årsaker = listOf(),
                forrigeBehandlingId = null
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

            MottattDokumentRepository(connection).lagre(
                MottattDokument(
                    referanse = InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "xxx"),
                    sakId = sak.id,
                    behandlingId = opprettetBehandling.id,
                    mottattTidspunkt = LocalDateTime.now().minusDays(1),
                    type = InnsendingType.SØKNAD,
                    kanal = Kanal.PAPIR,
                    strukturertDokument = null
                )
            )

            behandlingRepository.oppdaterBehandlingStatus(opprettetBehandling.id, Status.AVSLUTTET)

            val oppdatertBehandling = behandlingRepository.hent(opprettetBehandling.id)

            Triple(oppdatertBehandling, sak, ident)
        }

        val hendelseTidspunkt = LocalDateTime.now()
        val payload = BehandlingFlytStoppetHendelse(
            personIdent = ident.identifikator,
            saksnummer = sak.saksnummer,
            referanse = behandling.referanse,
            behandlingType = behandling.typeBehandling(),
            status = behandling.status(),
            avklaringsbehov = listOf(),
            opprettetTidspunkt = LocalDateTime.now(),
            hendelsesTidspunkt = hendelseTidspunkt,
            versjon = "123"
        )

        val hendelse2 = DefaultJsonMapper.toJson(payload)

        // Act

        InitTestDatabase.dataSource.transaction { connection ->
            val sakService = SakService(SakRepositoryImpl(connection))
            val vilkårsResultatRepository = VilkårsresultatRepository(connection = connection)
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val beregningsgrunnlagRepository = BeregningsgrunnlagRepository(connection)

            StatistikkJobbUtfører(
                StatistikkGateway(),
                vilkårsResultatRepository,
                behandlingRepository,
                sakService,
                TilkjentYtelseRepository(connection),
                beregningsgrunnlagRepository,
                PipRepository(connection),
                MottattDokumentRepository(connection)
            ).utfør(
                JobbInput(StatistikkJobbUtfører).medPayload(hendelse2)
            )
        }

        // Assert

        assertThat(hendelser).isNotEmpty()
        assertThat(hendelser.first().avsluttetBehandling.toString()).isEqualTo(
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
                            vilkårType = no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype.valueOf(Vilkårtype.MEDLEMSKAP.toString()),
                            perioder = listOf(
                                VilkårsPeriodeDTO(
                                    fraDato = LocalDate.now().minusDays(1),
                                    tilDato = LocalDate.now().plusDays(1),
                                    utfall = no.nav.aap.behandlingsflyt.kontrakt.statistikk.Utfall.valueOf(Utfall.OPPFYLT.toString()),
                                    manuellVurdering = false,
                                    avslagsårsak = null,
                                    innvilgelsesårsak = "null",
                                )
                            )
                        )
                    )
                ),
            ).toString()
        )
    }

    @Test
    fun `prosesserings-kall avgir statistikk korrekt`(hendelser: List<StoppetBehandling>) {
        val referanse = BehandlingReferanse()

        // Blir ikke kalt i denne metoden, så derfor bare mock
        val vilkårsResultatRepository = mockk<VilkårsresultatRepository>()
        val behandlingRepository = mockk<BehandlingRepository>()
        val behandlingId = BehandlingId(0)
        val sakId = SakId(1)
        every {
            behandlingRepository.hent(referanse)
        }.returns(
            Behandling(
                id = behandlingId,
                sakId = sakId,
                typeBehandling = TypeBehandling.Klage,
                versjon = 1,
                forrigeBehandlingId = null
            )
        )

        val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
        val beregningsgrunnlagRepository = mockk<BeregningsgrunnlagRepository>()
        val sakService = mockk<SakService>()

        every { sakService.hent(Saksnummer.valueOf(sakId.id)) } returns Sak(
            id = sakId,
            saksnummer = Saksnummer.valueOf(sakId.id),
            person = mockk(),
            rettighetsperiode = mockk(),
            status = UTREDES,
            opprettetTidspunkt = LocalDateTime.now(),
        )
        val dokumentRepository = mockk<MottattDokumentRepository>()

        val nå = LocalDateTime.now()
        val tidligsteMottattTid = nå.minusDays(3)
        // Mottatt tid defineres som tidligste mottatt-tidspunkt på innsendte søknader.
        every {
            dokumentRepository.hentDokumenterAvType(sakId, InnsendingType.SØKNAD)
        }.returns(
            setOf(
                MottattDokument(
                    referanse = InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "xxx"),
                    sakId = sakId,
                    behandlingId = behandlingId,
                    mottattTidspunkt = nå.minusDays(1),
                    type = InnsendingType.SØKNAD,
                    kanal = Kanal.DIGITAL,
                    strukturertDokument = null
                ),
                MottattDokument(
                    referanse = InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "xxx2"),
                    sakId = sakId,
                    behandlingId = behandlingId,
                    mottattTidspunkt = tidligsteMottattTid,
                    type = InnsendingType.SØKNAD,
                    kanal = Kanal.PAPIR,
                    strukturertDokument = null
                )
            )
        )

        val pipRepository = mockk<PipRepository>()
        every { pipRepository.finnIdenterPåSak(any()) } returns listOf(
            PipRepository.IdentPåSak(
                ident = "123",
                opprinnelse = PipRepository.IdentPåSak.Opprinnelse.PERSON
            )
        )

        val utfører =
            StatistikkJobbUtfører(
                StatistikkGateway(),
                vilkårsResultatRepository,
                behandlingRepository,
                sakService,
                tilkjentYtelseRepository,
                beregningsgrunnlagRepository,
                pipRepository,
                dokumentRepository
            )

        val avklaringsbehov = listOf(
            AvklaringsbehovHendelseDto(
                definisjon = DefinisjonDTO(
                    type = AvklaringsbehovKode.`5050`,
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

        val fødselsNummer = Ident("xxx").toString()
        val hendelsesTidspunkt = LocalDateTime.now()
        val payload = BehandlingFlytStoppetHendelse(
            saksnummer = Saksnummer.valueOf(sakId.id),
            personIdent = fødselsNummer,
            status = Status.UTREDES,
            behandlingType = TypeBehandling.Klage,
            referanse = referanse,
            opprettetTidspunkt = LocalDateTime.now(),
            avklaringsbehov = avklaringsbehov,
            hendelsesTidspunkt = hendelsesTidspunkt,
            versjon = ApplikasjonsVersjon.versjon
        )

        val hendelse = DefaultJsonMapper.toJson(payload)

        // Act
        utfører.utfør(
            JobbInput(StatistikkJobbUtfører).medPayload(hendelse)
        )

        // Assert
        assertThat(hendelser).isNotEmpty()
        assertThat(hendelser.size).isEqualTo(1)

        assertThat(hendelser.first()).isEqualTo(
            StoppetBehandling(
                saksnummer = Saksnummer.valueOf(sakId.id).toString(),
                behandlingReferanse = referanse.referanse,
                behandlingStatus = Status.UTREDES,
                behandlingType = TypeBehandling.Klage,
                ident = fødselsNummer,
                avklaringsbehov = avklaringsbehov,
                behandlingOpprettetTidspunkt = payload.opprettetTidspunkt,
                versjon = ApplikasjonsVersjon.versjon,
                soknadsFormat = Kanal.PAPIR,
                mottattTid = tidligsteMottattTid,
                sakStatus = no.nav.aap.behandlingsflyt.kontrakt.sak.Status.UTREDES,
                hendelsesTidspunkt = hendelsesTidspunkt,
                identerForSak = listOf("123")
            )
        )

        checkUnnecessaryStub(
            sakService,
            beregningsgrunnlagRepository,
            tilkjentYtelseRepository,
            behandlingRepository,
            vilkårsResultatRepository,
            dokumentRepository
        )
    }
}