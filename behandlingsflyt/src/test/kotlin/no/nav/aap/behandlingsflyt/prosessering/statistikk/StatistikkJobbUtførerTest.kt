package no.nav.aap.behandlingsflyt.prosessering.statistikk

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.IKlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.integrasjon.statistikk.StatistikkGatewayImpl
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.AvklaringsbehovHendelseDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.EndringDTO
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status.UTREDES
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.AvsluttetBehandlingDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.BeregningsgrunnlagDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Diagnoser
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Grunnlag11_19DTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ResultatKode
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetstypePeriode
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StoppetBehandling
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.TilkjentYtelseDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.VilkårDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.VilkårsPeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.VilkårsResultatDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom.SykdomRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvbrytRevurderingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMottattDokumentRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryPipRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTrukketSøknadRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryUnderveisRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVilkårsresultatRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.motor.JobbInput
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@Fakes
class StatistikkJobbUtførerTest {
    private val dataSource1 = InitTestDatabase.freshDatabase()

    @Test
    fun `mottatt tidspunkt er korrekt når revurdering`(hendelser: List<StoppetBehandling>) {
        var opprettetTidspunkt: LocalDateTime? = null
        val dataSource = dataSource1
        val (behandling, sak, ident) = dataSource.transaction { connection ->
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
            val sak = PersonOgSakService(
                identGateway,
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            ).finnEllerOpprett(
                ident, periode = Periode(LocalDate.now().minusDays(10), LocalDate.now().plusDays(1))
            )

            val opprettetBehandling = behandlingRepository.opprettBehandling(
                sak.id,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(
                        VurderingsbehovMedPeriode(
                            no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.MOTTATT_SØKNAD
                        )
                    ),
                    årsak = ÅrsakTilOpprettelse.SØKNAD
                )
            )

            val revurdering = behandlingRepository.opprettBehandling(
                sak.id,
                typeBehandling = TypeBehandling.Revurdering,
                forrigeBehandlingId = opprettetBehandling.id,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(
                        VurderingsbehovMedPeriode(
                            no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.MOTTATT_SØKNAD
                        )
                    ),
                    årsak = ÅrsakTilOpprettelse.SØKNAD
                )
            )

            opprettetTidspunkt = revurdering.opprettetTidspunkt

            MottattDokumentRepositoryImpl(connection).lagre(
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
            avklaringsbehov = emptyList(),
            erPåVent = false,
            relevanteIdenterPåBehandling = emptyList(),
            opprettetTidspunkt = opprettetTidspunkt!!,
            hendelsesTidspunkt = hendelseTidspunkt,
            versjon = "123",
            årsakerTilBehandling = listOf(Vurderingsbehov.SØKNAD.name),
            vurderingsbehov = listOf(Vurderingsbehov.SØKNAD.name),
            mottattDokumenter = emptyList(),
            årsakTilOpprettelse = behandling.årsakTilOpprettelse?.name ?: "Ukjent",
        )

        val hendelse2 = DefaultJsonMapper.toJson(payload)

        // Act

        dataSource.transaction { connection ->
            StatistikkJobbUtfører(
                statistikkMetoder = StatistikkMetoder(postgresRepositoryRegistry.provider(connection)),
                statistikkGateway = StatistikkGatewayImpl(),
            ).utfør(
                JobbInput(StatistikkJobbUtfører).medPayload(hendelse2)
            )
        }

        // Assert

        assertThat(hendelser).isNotEmpty()
        assertThat(hendelser.size).isEqualTo(1)
        assertThat(hendelser.first().mottattTid.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(
            opprettetTidspunkt.truncatedTo(
                ChronoUnit.SECONDS
            )
        )
    }

    @Test
    fun `statistikk-jobb avgir avsluttet behandling-data korrekt`(hendelser: List<StoppetBehandling>) {
        val periode = Periode(
            fom = LocalDate.now().minusDays(1),
            tom = LocalDate.now().plusDays(1)
        )
        val dataSource = dataSource1
        val (behandling, sak, ident) = dataSource.transaction { connection ->
            val vilkårsResultatRepository = VilkårsresultatRepositoryImpl(connection = connection)
            val behandlingRepository = BehandlingRepositoryImpl(connection)

            val beregningsgrunnlagRepository = BeregningsgrunnlagRepositoryImpl(connection)

            val ident = Ident(
                identifikator = "123",
                aktivIdent = true
            )
            val identGateway = object : IdentGateway {
                override fun hentAlleIdenterForPerson(ident: Ident): List<Ident> {
                    return listOf(ident)
                }
            }

            val sak = PersonOgSakService(
                identGateway,
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            ).finnEllerOpprett(
                ident, periode = Periode(LocalDate.now().minusDays(10), LocalDate.now().plusDays(1))
            )

            val opprettetBehandling = behandlingRepository.opprettBehandling(
                sak.id,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    årsak = ÅrsakTilOpprettelse.SØKNAD,
                    vurderingsbehov = listOf(
                        VurderingsbehovMedPeriode(
                            no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.MOTTATT_SØKNAD
                        )
                    ),
                ),
            )
            beregningsgrunnlagRepository.lagre(
                behandlingId = opprettetBehandling.id,
                Grunnlag11_19(
                    grunnlaget = GUnit(7),
                    erGjennomsnitt = false,
                    gjennomsnittligInntektIG = GUnit(0),
                    inntekter = emptyList()
                )
            )

            val vilkårsresultat = Vilkårsresultat(
                vilkår = listOf(
                    Vilkår(
                        type = Vilkårtype.MEDLEMSKAP, vilkårsperioder = setOf(
                            Vilkårsperiode(
                                periode = periode,
                                utfall = Utfall.OPPFYLT,
                                manuellVurdering = false,
                                begrunnelse = "ignorert",
                                innvilgelsesårsak = null,
                                avslagsårsak = null,
                                faktagrunnlag = null,
                                versjon = "123"
                            )
                        )
                    )
                )
            )
            vilkårsResultatRepository.lagre(
                opprettetBehandling.id, vilkårsresultat
            )

            MottattDokumentRepositoryImpl(connection).lagre(
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

            SykdomRepositoryImpl(connection).lagre(
                behandlingId = opprettetBehandling.id, listOf(
                    Sykdomsvurdering(
                        begrunnelse = "123",
                        dokumenterBruktIVurdering = emptyList(),
                        harSkadeSykdomEllerLyte = true,
                        erSkadeSykdomEllerLyteVesentligdel = true,
                        erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                        erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true,
                        yrkesskadeBegrunnelse = "begr",
                        erArbeidsevnenNedsatt = true,
                        kodeverk = "KODEVERK",
                        hoveddiagnose = "PEST",
                        bidiagnoser = listOf("KOLERA"),
                        vurderingenGjelderFra = null,
                        vurdertAv = Bruker("Z0000"),
                        opprettet = Instant.now(),
                    )
                )
            )

            UnderveisRepositoryImpl(connection).lagre(
                behandlingId = opprettetBehandling.id,
                underveisperioder = listOf(
                    Underveisperiode(
                        periode = periode,
                        meldePeriode = periode,
                        utfall = Utfall.OPPFYLT,
                        rettighetsType = RettighetsType.STUDENT,
                        grenseverdi = Prosent.`100_PROSENT`,
                        arbeidsgradering = ArbeidsGradering(
                            totaltAntallTimer = TimerArbeid(BigDecimal(22)),
                            andelArbeid = Prosent(33),
                            fastsattArbeidsevne = Prosent(23),
                            gradering = Prosent(23),
                            opplysningerMottatt = null,
                        ),
                        trekk = Dagsatser(0),
                        brukerAvKvoter = setOf(Kvote.STUDENT, Kvote.ORDINÆR),
                        avslagsårsak = null,
                        institusjonsoppholdReduksjon = Prosent.`0_PROSENT`,
                        meldepliktStatus = MeldepliktStatus.MELDT_SEG,
                    )
                ),
                input = object : Faktagrunnlag {}
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
            avklaringsbehov = emptyList(),
            erPåVent = false,
            relevanteIdenterPåBehandling = emptyList(),
            opprettetTidspunkt = LocalDateTime.now(),
            hendelsesTidspunkt = hendelseTidspunkt,
            versjon = "123",
            årsakerTilBehandling = listOf(Vurderingsbehov.VURDER_RETTIGHETSPERIODE.name),
            vurderingsbehov = listOf(Vurderingsbehov.VURDER_RETTIGHETSPERIODE.name),
            årsakTilOpprettelse = behandling.årsakTilOpprettelse?.name ?: "Ukjent",
            reserverTil = "meg",
            mottattDokumenter = emptyList()
        )

        val hendelse2 = DefaultJsonMapper.toJson(payload)

        // Act

        dataSource.transaction { connection ->
            StatistikkJobbUtfører(
                statistikkGateway = StatistikkGatewayImpl(),
                statistikkMetoder = StatistikkMetoder(postgresRepositoryRegistry.provider(connection)),
            ).utfør(
                JobbInput(StatistikkJobbUtfører).medPayload(hendelse2)
            )
        }

        // Assert

        assertThat(hendelser).isNotEmpty()
        assertThat(hendelser.first().avsluttetBehandling.toString()).isEqualTo(
            AvsluttetBehandlingDTO(
                tilkjentYtelse = TilkjentYtelseDTO(perioder = emptyList()),
                beregningsGrunnlag = BeregningsgrunnlagDTO(
                    grunnlag11_19dto = Grunnlag11_19DTO(
                        inntekter = emptyMap(),
                        grunnlaget = 7.0,
                        er6GBegrenset = false,
                        erGjennomsnitt = false,
                    )
                ),
                vilkårsResultat =
                    VilkårsResultatDTO(
                        typeBehandling = TypeBehandling.Førstegangsbehandling,
                        vilkår = listOf(
                            VilkårDTO(
                                vilkårType = no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype.valueOf(
                                    Vilkårtype.MEDLEMSKAP.toString()
                                ),
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
                diagnoser = Diagnoser(kodeverk = "KODEVERK", diagnosekode = "PEST", bidiagnoser = listOf("KOLERA")),
                rettighetstypePerioder = listOf(
                    RettighetstypePeriode(
                        fraDato = periode.fom,
                        tilDato = periode.tom,
                        rettighetstype = no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType.STUDENT
                    )
                ),
                resultat = ResultatKode.INNVILGET,
            ).toString()
        )
    }

    @Test
    fun `prosesserings-kall avgir statistikk korrekt`(hendelser: List<StoppetBehandling>) {
        val vilkårsResultatRepository = InMemoryVilkårsresultatRepository
        val behandlingRepository = InMemoryBehandlingRepository

        val sak = InMemorySakRepository.finnEllerOpprett(
            Person(
                identifikator = UUID.randomUUID(),
                identer = listOf(
                    Ident(
                        identifikator = "1234",
                        aktivIdent = true
                    )
                )
            ), Periode(LocalDate.now(), LocalDate.now().plusDays(1))
        )
        InMemorySakRepository.oppdaterSakStatus(sak.id, UTREDES)
        val sakId = sak.id
        val behandling = behandlingRepository.opprettBehandling(
            sakId = sakId,
            typeBehandling = TypeBehandling.Klage,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(
                    VurderingsbehovMedPeriode(
                        type = no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.MOTTATT_SØKNAD,
                        periode = Periode(LocalDate.now(), LocalDate.now().plusDays(1))
                    )
                ),
                årsak = ÅrsakTilOpprettelse.SØKNAD
            )
        )
        val referanse = behandling.referanse

        val tilkjentYtelseRepository = InMemoryTilkjentYtelseRepository

        val beregningsgrunnlagRepository = InMemoryBeregningsgrunnlagRepository
        val sakService = SakService(InMemorySakRepository)

        val nå = LocalDateTime.now()
        val tidligsteMottattTid = nå.minusDays(3)

        val dokumentRepository = InMemoryMottattDokumentRepository
        InMemoryMottattDokumentRepository.lagre(
            MottattDokument(
                referanse = InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "xxx"),
                sakId = sakId,
                behandlingId = behandling.id,
                mottattTidspunkt = nå.minusDays(1),
                type = InnsendingType.SØKNAD,
                kanal = Kanal.DIGITAL,
                strukturertDokument = null
            )
        )
        InMemoryMottattDokumentRepository.lagre(
            MottattDokument(
                referanse = InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "xxx2"),
                sakId = sakId,
                behandlingId = behandling.id,
                mottattTidspunkt = tidligsteMottattTid,
                type = InnsendingType.SØKNAD,
                kanal = Kanal.PAPIR,
                strukturertDokument = null
            )
        )

        val sykdomRepository = object : SykdomRepository {
            override fun lagre(behandlingId: BehandlingId, sykdomsvurderinger: List<Sykdomsvurdering>) {
                TODO("Not yet implemented")
            }

            override fun lagre(behandlingId: BehandlingId, yrkesskadevurdering: Yrkesskadevurdering?) {
                TODO("Not yet implemented")
            }

            override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
                TODO("Not yet implemented")
            }

            override fun hentHvisEksisterer(behandlingId: BehandlingId): SykdomGrunnlag? {
                TODO("Not yet implemented")
            }

            override fun hent(behandlingId: BehandlingId): SykdomGrunnlag {
                TODO("Not yet implemented")
            }

            override fun slett(behandlingId: BehandlingId) {
            }

            override fun hentHistoriskeSykdomsvurderinger(
                sakId: SakId,
                behandlingId: BehandlingId
            ): List<Sykdomsvurdering> {
                TODO("Not yet implemented")
            }
        }

        val utfører =
            StatistikkJobbUtfører(
                statistikkGateway = StatistikkGatewayImpl(),
                statistikkMetoder = StatistikkMetoder(
                    vilkårsresultatRepository =vilkårsResultatRepository ,
                    behandlingRepository = behandlingRepository,
                    sakService = sakService,
                    tilkjentYtelseRepository = tilkjentYtelseRepository,
                    beregningsgrunnlagRepository = beregningsgrunnlagRepository,
                    pipRepository = InMemoryPipRepository,
                    dokumentRepository = dokumentRepository,
                    sykdomRepository = sykdomRepository,
                    underveisRepository = InMemoryUnderveisRepository,
                    trukketSøknadService = TrukketSøknadService(
                        trukketSøknadRepository = InMemoryTrukketSøknadRepository
                    ),
                    klageresultatUtleder = DummyKlageresultatUtleder(),
                    avbrytRevurderingService = AvbrytRevurderingService(InMemoryAvbrytRevurderingRepository),
                )
            )

        val avklaringsbehov = listOf(
            AvklaringsbehovHendelseDto(
                avklaringsbehovDefinisjon = Definisjon.FATTE_VEDTAK,
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
            relevanteIdenterPåBehandling = emptyList(),
            erPåVent = false,
            hendelsesTidspunkt = hendelsesTidspunkt,
            versjon = ApplikasjonsVersjon.versjon,
            årsakerTilBehandling = listOf(Vurderingsbehov.VURDER_RETTIGHETSPERIODE.name),
            vurderingsbehov = listOf(Vurderingsbehov.VURDER_RETTIGHETSPERIODE.name),
            årsakTilOpprettelse = behandling.årsakTilOpprettelse?.name ?: "Ukjent",
            mottattDokumenter = emptyList(),
            reserverTil = "meg",
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
                sakStatus = UTREDES,
                hendelsesTidspunkt = hendelsesTidspunkt,
                identerForSak = listOf("123"),
                vurderingsbehov = listOf(Vurderingsbehov.SØKNAD)
            )
        )
    }
}

class DummyKlageresultatUtleder : IKlageresultatUtleder {
    override fun utledKlagebehandlingResultat(behandlingId: BehandlingId): KlageResultat {
        throw NotImplementedError("Lag spesifikk implementasjon for caset du vil teste")
    }
}