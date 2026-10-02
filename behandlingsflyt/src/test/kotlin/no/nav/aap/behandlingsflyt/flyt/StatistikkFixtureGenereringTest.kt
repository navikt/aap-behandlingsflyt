package no.nav.aap.behandlingsflyt.flyt

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.JsonNode
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov as FlytVurderingsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.ÅrsakTilRetur
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.ÅrsakTilReturKode
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettBeregningstidspunktLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurderingDto
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.flate.AvbrytRevurderingVurderingDto
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.flate.AvbrytRevurderingÅrsakDto
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvbrytRevurderingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarYrkesskadeLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.YrkesskadeSakDto
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.YrkesskadevurderingDto
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status as AvklaringsbehovStatus
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StoppetBehandling
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkGateway
import no.nav.aap.behandlingsflyt.hendelse.avløp.sortererteAvklaringsbehov
import no.nav.aap.behandlingsflyt.prosessering.statistikk.BehandlingFlytStoppetHendelseTilStatistikk
import no.nav.aap.behandlingsflyt.prosessering.statistikk.ResendStatistikkJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.statistikk.StatistikkMetoder
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.ZoneOffset

@Tag("statistikk-fixtures")
class StatistikkFixtureGenereringTest : AbstraktFlytOrkestratorTest(AlleAvskruddUnleash::class) {
    @Test
    fun `generer fullført førstegangsbehandling`() {
        val sak = happyCaseFørstegangsbehandling(sendMeldekort = false)
        val behandling = hentSisteOpprettedeBehandlingForSak(sak.id)
        val hendelse = hendelseFraBehandling(behandling)
        assertThat(hendelse.behandlingStatus).isEqualTo(Status.AVSLUTTET)
        assertThat(hendelse.avklaringsbehov.last().avklaringsbehovDefinisjon).isEqualTo(Definisjon.SKRIV_VEDTAKSBREV)
        assertThat(hendelse.avklaringsbehov.last().endringer.last().endretAv).isEqualTo("BESLUTTER")
        skrivFixture("fullfort_forstegangsbehandling.json", hendelse)
    }

    @Test
    fun `generer mellomstadier i førstegangsbehandling`() {
        happyCaseFørstegangsbehandling(
            sendMeldekort = false,
            etterKvalitetssikring = { behandling ->
                val hendelse = hendelseFraBehandling(behandling)
                assertThat(hendelse.behandlingStatus).isEqualTo(Status.UTREDES)
                assertThat(hendelse.avklaringsbehov.single { it.avklaringsbehovDefinisjon == Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT }.status)
                        .isEqualTo(AvklaringsbehovStatus.OPPRETTET)
                assertThat(hendelse.avklaringsbehov.single { it.avklaringsbehovDefinisjon == Definisjon.KVALITETSSIKRING }.status)
                        .isEqualTo(AvklaringsbehovStatus.AVSLUTTET)
                assertThat(hendelse.avklaringsbehov.single { it.avklaringsbehovDefinisjon == Definisjon.SKRIV_SYKDOMSVURDERING_BREV }.status)
                        .isEqualTo(AvklaringsbehovStatus.KVALITETSSIKRET)
                skrivFixture("grunnlag_steg.json", hendelse)
            },
            etterFattVedtak = { behandling ->
                val hendelse = hendelseFraBehandling(behandling)
                assertThat(hendelse.behandlingStatus).isEqualTo(Status.IVERKSETTES)
                assertThat(hendelse.avklaringsbehov.single { it.avklaringsbehovDefinisjon == Definisjon.SKRIV_VEDTAKSBREV }.status)
                        .isEqualTo(AvklaringsbehovStatus.OPPRETTET)
                assertThat(hendelse.avklaringsbehov.single { it.avklaringsbehovDefinisjon == Definisjon.FATTE_VEDTAK }.status)
                        .isEqualTo(AvklaringsbehovStatus.AVSLUTTET)
                skrivFixture("er_pa_brev_steget.json", hendelse)
            }
        )
    }

    @Test
    fun `generer sendt tilbake fra beslutter på 11-5`() {
        val fom = LocalDate.now()
        val (_, behandling) = sendInnFørsteSøknad(
            mottattTidspunkt = fom.atStartOfDay(),
            person = TestPersoner.STANDARD_PERSON(),
            søknad = TestSøknader.STANDARD_SØKNAD
        )
        val returnert = behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsAvklaringsBehov(
                FastsettBeregningstidspunktLøsning(
                    beregningVurdering = BeregningstidspunktVurderingDto(
                        begrunnelse = "Trenger hjelp fra Nav",
                        nedsattArbeidsevneDato = fom,
                        ytterligereNedsattArbeidsevneDato = null,
                        ytterligereNedsattBegrunnelse = null
                    ),
                ),
            )
            .løsOppholdskrav(fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .beslutterGodkjennerIkke(
                underkjennVurderinger = listOf(Definisjon.AVKLAR_SYKDOM),
                grunner = listOf(ÅrsakTilRetur(ÅrsakTilReturKode.MANGLENDE_UTREDNING, null))
            )

        val hendelse = hendelseFraBehandling(returnert)
        assertThat(hendelse.behandlingStatus).isEqualTo(Status.UTREDES)
        val sykdom = hendelse.avklaringsbehov.single { it.avklaringsbehovDefinisjon == Definisjon.AVKLAR_SYKDOM }
        assertThat(sykdom.status).isEqualTo(AvklaringsbehovStatus.SENDT_TILBAKE_FRA_BESLUTTER)
        assertThat(sykdom.endringer.last().endretAv).isEqualTo("BESLUTTER")
        assertThat(sykdom.endringer.last().årsakTilRetur.map { it.årsak.name }).containsExactly("MANGLENDE_UTREDNING")
        val fatteVedtak = hendelse.avklaringsbehov.single { it.avklaringsbehovDefinisjon == Definisjon.FATTE_VEDTAK }
        assertThat(fatteVedtak.status).isEqualTo(AvklaringsbehovStatus.AVSLUTTET)
        assertThat(fatteVedtak.endringer.last().endretAv).isEqualTo("BESLUTTER")
        // Siden #2492 gjenåpner ikke FatteVedtakLøser senere behov ved retur; kun det underkjente behovet er åpent.
//        assertThat(hendelse.avklaringsbehov.filter { it.status.erÅpent() }).containsExactly(sykdom)
        skrivFixture("sendt_tilbake_11_5_fra_beslutter.json", hendelse)
    }

    @Test
    fun `generer avbrutt revurdering`() {
        val person = TestPersoner.PERSON_MED_YRKESSKADE()
        val (sak, behandling) = sendInnFørsteSøknad(person = person)
        behandling.løsSykdom(sak.rettighetsperiode.fom)
            .løsBistand(sak.rettighetsperiode.fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsAvklaringsBehov(
                AvklarYrkesskadeLøsning(
                    yrkesskadesvurdering = YrkesskadevurderingDto(
                        begrunnelse = "Relevant yrkesskade",
                        relevanteYrkesskadeSaker = person.yrkesskade.map { YrkesskadeSakDto(it.saksreferanse, null) },
                        andelAvNedsettelsen = 50,
                        erÅrsakssammenheng = true
                    )
                )
            )
            .løsBeregningstidspunkt()
            .løsYrkesskadeInntekt(person.yrkesskade)
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_INNVILGELSE)

        val revurdering = sak.opprettManuellRevurdering(listOf(Vurderingsbehov.REVURDER_BEREGNING))
        val åpneBehov = hentAlleAvklaringsbehov(revurdering).filter { it.status() == AvklaringsbehovStatus.OPPRETTET }
        assertThat(åpneBehov.map { it.definisjon })
            .containsExactlyInAnyOrder(Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT, Definisjon.FASTSETT_YRKESSKADEINNTEKT)

        revurdering.leggTilVurderingsbehov(Vurderingsbehov.REVURDERING_AVBRUTT)
            .løsAvklaringsBehov(
                AvbrytRevurderingLøsning(
                    vurdering = AvbrytRevurderingVurderingDto(
                        årsak = AvbrytRevurderingÅrsakDto.REVURDERINGEN_BLE_OPPRETTET_VED_EN_FEIL,
                        begrunnelse = "Fordi den ikke er aktuell lenger"
                    )
                )
            )

        val hendelse = hendelseFraBehandling(hentBehandling(revurdering.referanse))
        assertThat(hendelse.behandlingStatus).isEqualTo(Status.AVSLUTTET)
        assertThat(hendelse.avklaringsbehov.associate { it.avklaringsbehovDefinisjon to it.status }).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                Definisjon.AVBRYT_REVURDERING to AvklaringsbehovStatus.AVSLUTTET,
                Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT to AvklaringsbehovStatus.AVBRUTT,
                Definisjon.FASTSETT_YRKESSKADEINNTEKT to AvklaringsbehovStatus.AVBRUTT
            )
        )
        skrivFixture("avbrutt_revurdering.json", hendelse)
    }

    @Test
    fun `generer meldekortbehandling`() {
        val sak = happyCaseFørstegangsbehandling(sendMeldekort = false)
        val timer = listOf(1.0, 0.0, 6.0, 7.0, 1.0, 3.0, 0.0, 7.5, 7.5, 3.5, 0.0, 0.0, 0.0, 0.0)
        sak.sendInnMeldekort(timer.mapIndexed { indeks, antall ->
            LocalDate.now().minusDays((14 - indeks).toLong()) to antall
        }.toMap())
        val behandling = hentSisteOpprettedeBehandlingForSak(sak.id)
        val hendelse = hendelseFraBehandling(behandling)
        assertThat(hendelse.vurderingsbehov).contains(Vurderingsbehov.MELDEKORT)
        assertThat(hendelse.behandlingStatus).isEqualTo(Status.AVSLUTTET)
        assertThat(hendelse.nyeMeldekort.single().arbeidIPeriode.map { it.timerArbeidet })
            .containsExactlyElementsOf(timer.map { it.toBigDecimal() })
        skrivFixture("meldekort_behandling.json", hendelse)
    }

    @Test
    fun `generer resendt førstegangsbehandling via resend-jobb`() {
        val sak = happyCaseFørstegangsbehandling(sendMeldekort = false, etterSøknad = { opprinneligSak, behandling ->
            behandling.bestillLegeerklæring()
            dataSource.transaction { connection ->
                FlytJobbRepository(connection).leggTil(
                    HendelseMottattHåndteringJobbUtfører.nyJobb(
                        sakId = opprinneligSak.id,
                        dokumentReferanse = InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "fixture-legeerklæring"),
                        brevkategori = InnsendingType.LEGEERKLÆRING,
                        kanal = Kanal.DIGITAL,
                        mottattTidspunkt = java.time.LocalDateTime.now()
                    )
                )
            }
            motor.kjørJobber()
        })
        val behandling = hentSisteOpprettedeBehandlingForSak(sak.id)
        val hendelse = resendHendelse(behandling)
        assertThat(hendelse.behandlingStatus).isEqualTo(Status.AVSLUTTET)
        assertThat(hendelse.behandlingType).isEqualTo(TypeBehandling.Førstegangsbehandling)
        assertThat(hendelse.avklaringsbehov).anySatisfy {
            assertThat(it.avklaringsbehovDefinisjon).isEqualTo(Definisjon.FATTE_VEDTAK)
        }
        assertThat(hendelse.avklaringsbehov).anySatisfy {
            assertThat(it.avklaringsbehovDefinisjon).isEqualTo(Definisjon.BESTILL_LEGEERKLÆRING)
            assertThat(it.status).isEqualTo(AvklaringsbehovStatus.AVSLUTTET)
        }
        skrivFixture("resendt_hendelse.json", hendelse)
    }

    private fun resendHendelse(behandling: Behandling): StoppetBehandling {
        val gateway = mockk<StatistikkGateway>(relaxed = true)
        val publisert = slot<StoppetBehandling>()
        dataSource.transaction { connection ->
            val repos = postgresRepositoryRegistry.provider(connection)
            ResendStatistikkJobbUtfører(
                behandlingRepository = repos.provide(),
                behandlingService = BehandlingService(repos, gatewayProvider),
                sakService = SakService(repos, gatewayProvider),
                avklaringsbehovRepository = repos.provide(),
                statistikkGateway = gateway,
                statistikkMetoder = StatistikkMetoder(repos, gatewayProvider),
            ).utfør(JobbInput(ResendStatistikkJobbUtfører).medPayload(behandling.id.id))
        }
        verify(exactly = 1) { gateway.resendBehandling(capture(publisert)) }
        return publisert.captured
    }

    @Test
    fun `generer automatisk effektuering av aktivitetsplikt`() {
        val sak = happyCaseFørstegangsbehandling(sendMeldekort = false)
        dataSource.transaction { connection ->
            val repos = postgresRepositoryRegistry.provider(connection)
            val aktivitet = BehandlingService(repos, gatewayProvider).opprettAktivitetspliktBehandling(
                sak.id, ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE, FlytVurderingsbehov.AKTIVITETSPLIKT_11_7
            )
            repos.provide<Aktivitetsplikt11_7Repository>().lagre(
                aktivitet.id, listOf(
                    Aktivitetsplikt11_7Vurdering(
                        begrunnelse = "Brudd på aktivitetsplikten",
                        erOppfylt = false,
                        utfall = Utfall.STANS,
                        fom = sak.rettighetsperiode.fom.plusWeeks(18),
                        vurdertAv = Bruker("Saksbehandler"),
                        opprettet = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC),
                        vurdertIBehandling = aktivitet.id,
                        skalIgnorereVarselFrist = false,
                    )
                )
            )
            repos.provide<BehandlingRepository>().oppdaterBehandlingStatus(aktivitet.id, Status.AVSLUTTET)
        }
        val opprettet = dataSource.transaction { connection ->
            BehandlingService(postgresRepositoryRegistry.provider(connection), gatewayProvider)
                .finnEllerOpprettBehandling(
                    sak.id,
                    VurderingsbehovOgÅrsak(
                        årsak = ÅrsakTilOpprettelse.AKTIVITETSPLIKT,
                        vurderingsbehov = listOf(VurderingsbehovMedPeriode(FlytVurderingsbehov.EFFEKTUER_AKTIVITETSPLIKT))
                    )
                )
        }
        assertThat(opprettet).isInstanceOf(BehandlingService.MåBehandlesAtomært::class.java)
        dataSource.transaction { connection ->
            ProsesserBehandlingService(postgresRepositoryRegistry.provider(connection), gatewayProvider)
                .triggProsesserBehandling(opprettet)
        }
        motor.kjørJobber()

        val hendelse = resendHendelse(hentSisteOpprettedeBehandlingForSak(sak.id))
        assertThat(hendelse.behandlingType).isEqualTo(TypeBehandling.Revurdering)
        assertThat(hendelse.behandlingStatus).isEqualTo(Status.AVSLUTTET)
        assertThat(hendelse.vurderingsbehov).contains(Vurderingsbehov.EFFEKTUER_AKTIVITETSPLIKT)
        assertThat(hendelse.avklaringsbehov).isEmpty()
        skrivFixture("resendt_revurdering_automatisk.json", hendelse)
    }

    private fun hendelseFraBehandling(behandling: Behandling): StoppetBehandling = dataSource.transaction { connection ->
        val sak = hentSak(behandling)
        StatistikkMetoder(postgresRepositoryRegistry.provider(connection), gatewayProvider)
            .oversettHendelseTilKontrakt(
                BehandlingFlytStoppetHendelseTilStatistikk(
                    personIdent = sak.person.aktivIdent().identifikator,
                    saksnummer = sak.saksnummer,
                    referanse = behandling.referanse,
                    behandlingType = behandling.typeBehandling(),
                    status = behandling.status(),
                    avklaringsbehov = sortererteAvklaringsbehov(behandling, hentAlleAvklaringsbehov(behandling)),
                    opprettetTidspunkt = behandling.opprettetTidspunkt,
                    hendelsesTidspunkt = java.time.LocalDateTime.now(),
                    versjon = "test"
                )
            )
    }

    private fun skrivFixture(navn: String, hendelse: StoppetBehandling) {
        val katalog = Path.of("build/statistikk-fixtures/avklaringsbehovhendelser")
        Files.createDirectories(katalog)
        val mapper = DefaultJsonMapper.objectMapper()
        val rot = normaliserStatistikkFixture(mapper.valueToTree(hendelse), hendelse.hendelsesTidspunkt.toLocalDate())
        val json = formaterStatistikkFixtureJson(rot)
        assertThat(DefaultJsonMapper.fromJson<StoppetBehandling>(json)).isEqualTo(mapper.treeToValue(rot, StoppetBehandling::class.java))
        Files.writeString(katalog.resolve(navn), "$json\n")
    }

    fun formaterStatistikkFixtureJson(node: JsonNode): String =
        DefaultJsonMapper.objectMapper().writer(DefaultPrettyPrinter()).writeValueAsString(node)
}
