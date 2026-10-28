package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.ARENA_MIGRERING_BRUKER
import no.nav.aap.behandlingsflyt.arena.ArenaBeregningstidspunkt
import no.nav.aap.behandlingsflyt.arena.ArenaForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektForutgåendeRepository
import no.nav.aap.behandlingsflyt.arena.ArenaMigreringsdataRepository
import no.nav.aap.behandlingsflyt.arena.ArenaOppholdskrav
import no.nav.aap.behandlingsflyt.arena.ArenaVedtaksperiode
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderKravLøsning
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ÅrsakBeregningstidspunkt
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertKravLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.test.FakeArenaOppslagGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertRettighetstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.AVSLUTTET
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MigreringFraArenaV0
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.fixedClock
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass

class MigreringFraArenaFlytTest : AbstraktFlytOrkestratorTest(AlleAvskruddUnleash::class) {

    private val clock = fixedClock(1 desember 2025)

    // Overstyrbar per test - default matcher konstruktørparameteren over
    private var unleash: KClass<out UnleashGateway> = AlleAvskruddUnleash::class
    override fun unleashGateway() = unleash

    @Test
    fun `skal kunne gjennomføre en migrering hvor stegene i en ordinær flyt må settes manuelt`() {
        val (sak, migreringsbehandling) = migrerFraArena()
        val startDato = sak.rettighetsperiode.fom

        migreringsbehandling
            .medKontekst {
                assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
                assertThat(behandling.vurderingsbehov().map { it.type })
                    .contains(no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.MIGRERING_FRA_ARENA)
            }
            .løsAvklaringsBehov(
                VurderKravLøsning(
                    kravVurderinger = setOf(
                        MigrertKravLøsningDto(
                            begrunnelse = "en begrunnelse",
                            virkningstidspunktArena = startDato.minusMonths(5),
                            muligRettFra = startDato,
                            arenaSaksnummer = "2016-123456",
                            rettighetstype = MigrertRettighetstype.ORDINÆR,
                            resterendeKvoteOrdinær = 300,
                        )
                    )
                )
            )
            .løsLovvalg(startDato, medlem = true)
            .løsSykdom(startDato, erOppfylt = true)
            .løsBistand(startDato, erOppfylt = true)
            .løsRefusjonskrav()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt(startDato)
            .løsFastsettManuellInntekt()
            .løsForutgåendeMedlemskap(startDato)
            .løsOppholdskrav(startDato)
            .løsBarnetillegg()
            .løsAndreStatligeYtelser()
            .løsVedtakslengde(startDato, startDato.plusMonths(6))
            .løsForeslåVedtak()
            .fattVedtak()
            .medKontekst {
                assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
                assertThat(behandling.vurderingsbehov().map { it.type })
                    .contains(no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.MIGRERING_FRA_ARENA)
                assertThat(avklaringsbehovSomMåLøses).isEmpty()
                assertThat(behandling.status()).isEqualTo(AVSLUTTET)
                behandling.assertRettighetstype(
                    Periode(startDato, startDato.plusMonths(6)) to RettighetsType.BISTANDSBEHOV
                )
            }
    }

    @Test
    fun `skal automatisk legge inn sykdom- og bistandsvurdering basert på sykdomsvurdering fra Arena i behandlingen`() {
        unleash = MigreringFraArenaMedAutomatiskSykdomFlytTestUnleash::class

        val (sak, migreringsbehandling) = migrerFraArena()
        val startDato = sak.rettighetsperiode.fom

        migreringsbehandling
            .løsAvklaringsBehov(
                VurderKravLøsning(
                    kravVurderinger = setOf(
                        MigrertKravLøsningDto(
                            begrunnelse = "en begrunnelse",
                            virkningstidspunktArena = startDato.minusMonths(5),
                            muligRettFra = startDato,
                            arenaSaksnummer = "2016-123456",
                            rettighetstype = MigrertRettighetstype.ORDINÆR,
                            resterendeKvoteOrdinær = 300,
                        )
                    )
                )
            )
            .løsLovvalg(startDato, medlem = true)
            .medKontekst {
                val medlemskapArbeidInntektRepository: SykdomRepository = repositoryProvider.provide()
                val vurderinger = medlemskapArbeidInntektRepository.hentHvisEksisterer(behandling.id)?.sykdomsvurderinger

                assertThat(behandling.aktivtSteg()).isEqualTo(StegType.REFUSJON_KRAV)

                assertThat(vurderinger).hasSize(1)
                val vurdering = vurderinger!!.single()
                assertThat(vurdering.erSkadeSykdomEllerLyteVesentligdel).isTrue
                assertThat(vurdering.erNedsettelseIArbeidsevneMerEnnHalvparten).isTrue
                assertThat(vurdering.harSkadeSykdomEllerLyte).isTrue
                assertThat(vurdering.harNedsattArbeidsevne).isEqualTo(ArbeidsevneNedsattValg.JA)
                assertThat(vurdering.fom).isEqualTo(sak.rettighetsperiode.fom)
                assertThat(vurdering.tom).isNull()
                assertThat(vurdering.vurdertAv).isEqualTo(ARENA_MIGRERING_BRUKER)
            }
    }

    @Test
    fun `skal automatisk legge inn vurderinger fra veileder i behandlingen`() {
        unleash = MigreringFraArenaMedAutomatiskeVurderingerFlytTestUnleash::class

        val (sak, migreringsbehandling) = migrerFraArena()
        val startDato = sak.rettighetsperiode.fom

        migreringsbehandling
            .løsLovvalg(startDato, medlem = true)
            .medKontekst {
                val medlemskapArbeidInntektRepository: SykdomRepository = repositoryProvider.provide()
                val vurderinger = medlemskapArbeidInntektRepository.hentHvisEksisterer(behandling.id)?.sykdomsvurderinger

                assertThat(behandling.aktivtSteg()).isEqualTo(StegType.FASTSETT_BEREGNINGSTIDSPUNKT)

                assertThat(vurderinger).hasSize(1)
                val vurdering = vurderinger!!.single()
                assertThat(vurdering.erSkadeSykdomEllerLyteVesentligdel).isTrue
                assertThat(vurdering.erNedsettelseIArbeidsevneMerEnnHalvparten).isTrue
                assertThat(vurdering.harSkadeSykdomEllerLyte).isTrue
                assertThat(vurdering.harNedsattArbeidsevne).isEqualTo(ArbeidsevneNedsattValg.JA)
                assertThat(vurdering.fom).isEqualTo(sak.rettighetsperiode.fom)
                assertThat(vurdering.tom).isNull()
            }
    }

    @Test
    fun `skal automatisk legge inn krav basert på kravdata fra Arena i behandlingen`() {
        unleash = MigreringFraArenaMedAutomatiskKravFlytTestUnleash::class

        val (sak, migreringsbehandling) = migrerFraArena()
        val startDato = sak.rettighetsperiode.fom
        val fraArena = FakeArenaOppslagGateway().hentKravDataForSak("2016-123456")

        migreringsbehandling
            .medKontekst {
                assertThat(avklaringsbehovSomMåLøses.map { it.definisjon }).doesNotContain(Definisjon.VURDER_KRAV)

                val kravRepository: KravRepository = repositoryProvider.provide()
                val krav = kravRepository.hentHvisEksisterer(behandling.id)!!.vurderinger.single() as MigrertKrav
                assertThat(krav.muligRettFra).isEqualTo(fraArena.migreringsdato)
                assertThat(krav.virkningstidspunktArena).isEqualTo(fraArena.soknadsdato)
                assertThat(krav.arenaSaksnummer).isEqualTo("${fraArena.aar}-${fraArena.lopenr}")
                assertThat(krav.resterendeKvoteOrdinær).isEqualTo(fraArena.gjenstaaendeKvote.ordinaer)
            }
            .løsLovvalg(startDato, medlem = true)
            .medKontekst {
                assertThat(behandling.aktivtSteg()).isEqualTo(StegType.AVKLAR_SYKDOM)
            }
    }

    @Test
    fun `skal automatisk legge inn oppfylt oppholdskrav i migreringsbehandlingen`() {
        unleash = MigreringFraArenaMedAutomatiskOppholdskravFlytTestUnleash::class

        val (sak, migreringsbehandling) = migrerFraArena()
        val startDato = sak.rettighetsperiode.fom

        migreringsbehandling
            .løsAvklaringsBehov(
                VurderKravLøsning(
                    kravVurderinger = setOf(
                        MigrertKravLøsningDto(
                            begrunnelse = "en begrunnelse",
                            virkningstidspunktArena = startDato.minusMonths(5),
                            muligRettFra = startDato,
                            arenaSaksnummer = "2016-123456",
                            rettighetstype = MigrertRettighetstype.ORDINÆR,
                            resterendeKvoteOrdinær = 300,
                        )
                    )
                )
            )
            .løsLovvalg(startDato, medlem = true)
            .løsSykdom(startDato, erOppfylt = true)
            .løsBistand(startDato, erOppfylt = true)
            .løsRefusjonskrav()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt(startDato)
            .løsFastsettManuellInntekt()
            .løsForutgåendeMedlemskap(startDato)
            .medKontekst {
                val oppholdskravRepository: OppholdskravGrunnlagRepository = repositoryProvider.provide()
                val vurdering = oppholdskravRepository.hentHvisEksisterer(behandling.id)!!.vurderinger.single()
                assertThat(vurdering.vurdertAv).isEqualTo(ARENA_MIGRERING_BRUKER)
                val periode = vurdering.perioder.single()
                assertThat(periode.oppfylt).isTrue
                assertThat(periode.fom).isEqualTo(startDato)
                assertThat(periode.tom).isNull()
                assertThat(avklaringsbehovSomMåLøses.map { it.definisjon }).doesNotContain(Definisjon.AVKLAR_OPPHOLDSKRAV)

                val migreringsdataRepository: ArenaMigreringsdataRepository = repositoryProvider.provide()
                val migreringsdata = migreringsdataRepository.hentAktivHvisEksisterer(
                    behandlingId = behandling.id,
                    steg = StegType.VURDER_OPPHOLDSKRAV,
                )
                assertThat(migreringsdata).isNotNull
                val lagretOppholdskrav =
                    DefaultJsonMapper.fromJson<ArenaOppholdskrav>(migreringsdata!!.data!!)
                assertThat(lagretOppholdskrav.vilkår?.status).isEqualTo("V")
            }
    }

    @Test
    fun `skal automatisk legge inn oppfylt forutgående medlemskap i migreringsbehandlingen`() {
        unleash = MigreringFraArenaMedAutomatiskForutgåendeMedlemskapFlytTestUnleash::class

        val (sak, migreringsbehandling) = migrerFraArena()
        val startDato = sak.rettighetsperiode.fom

        migreringsbehandling
            .løsAvklaringsBehov(
                VurderKravLøsning(
                    kravVurderinger = setOf(
                        MigrertKravLøsningDto(
                            begrunnelse = "en begrunnelse",
                            virkningstidspunktArena = startDato.minusMonths(5),
                            muligRettFra = startDato,
                            arenaSaksnummer = "2016-123456",
                            rettighetstype = MigrertRettighetstype.ORDINÆR,
                            resterendeKvoteOrdinær = 300,
                        )
                    )
                )
            )
            .løsLovvalg(startDato, medlem = true)
            .løsSykdom(startDato, erOppfylt = true)
            .løsBistand(startDato, erOppfylt = true)
            .løsRefusjonskrav()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt(startDato)
            .løsFastsettManuellInntekt()
            .medKontekst {
                val repository: MedlemskapArbeidInntektForutgåendeRepository = repositoryProvider.provide()
                val vurdering = repository.hentHvisEksisterer(behandling.id)!!.vurderinger.single()
                assertThat(vurdering.vurdertAv).isEqualTo(ARENA_MIGRERING_BRUKER)
                assertThat(vurdering.harForutgåendeMedlemskap).isTrue()
                assertThat(vurdering.fom).isEqualTo(startDato)
                assertThat(vurdering.tom).isNull()
                assertThat(avklaringsbehovSomMåLøses.map { it.definisjon })
                    .doesNotContain(Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP)

                val migreringsdataRepository: ArenaMigreringsdataRepository = repositoryProvider.provide()
                val migreringsdata = migreringsdataRepository.hentAktivHvisEksisterer(
                    behandlingId = behandling.id,
                    steg = StegType.VURDER_MEDLEMSKAP,
                )
                assertThat(migreringsdata).isNotNull
                val lagret = DefaultJsonMapper.fromJson<ArenaForutgåendeMedlemskap>(migreringsdata!!.data!!)
                assertThat(lagret.vilkår?.kode).isEqualTo("MEDLSKAP5")
                assertThat(lagret.vilkår?.status).isEqualTo("J")
            }
    }

    @Test
    fun `skal automatisk legge inn vedtakslengde fra Arena i migreringsbehandlingen`() {
        unleash = MigreringFraArenaMedAutomatiskVedtakslengdeFlytTestUnleash::class

        val (sak, migreringsbehandling) = migrerFraArena()
        val startDato = sak.rettighetsperiode.fom

        migreringsbehandling
            .løsAvklaringsBehov(
                VurderKravLøsning(
                    kravVurderinger = setOf(
                        MigrertKravLøsningDto(
                            begrunnelse = "en begrunnelse",
                            virkningstidspunktArena = startDato.minusMonths(5),
                            muligRettFra = startDato,
                            arenaSaksnummer = "2016-123456",
                            rettighetstype = MigrertRettighetstype.ORDINÆR,
                            resterendeKvoteOrdinær = 300,
                        )
                    )
                )
            )
            .løsLovvalg(startDato, medlem = true)
            .løsSykdom(startDato, erOppfylt = true)
            .løsBistand(startDato, erOppfylt = true)
            .løsRefusjonskrav()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt(startDato)
            .løsFastsettManuellInntekt()
            .løsForutgåendeMedlemskap(startDato)
            .løsOppholdskrav(startDato)
            .løsBarnetillegg()
            .løsAndreStatligeYtelser()
            .medKontekst {
                val vedtakslengdeRepository: VedtakslengdeRepository = repositoryProvider.provide()
                val vurdering = vedtakslengdeRepository.hentHvisEksisterer(behandling.id)!!.gjeldendeVurdering()!!
                assertThat(vurdering.vurdertAv).isEqualTo(ARENA_MIGRERING_BRUKER)
                assertThat(vurdering.sluttdato).isEqualTo(LocalDate.now().plusYears(1))
                assertThat(vurdering.utvidetMed).isEqualTo(ÅrMedHverdager.FØRSTE_ÅR)
                assertThat(avklaringsbehovSomMåLøses.map { it.definisjon }).doesNotContain(Definisjon.AVKLAR_VEDTAKSLENGDE)

                val migreringsdataRepository: ArenaMigreringsdataRepository = repositoryProvider.provide()
                val migreringsdata = migreringsdataRepository.hentAktivHvisEksisterer(
                    behandlingId = behandling.id,
                    steg = StegType.FASTSETT_VEDTAKSLENGDE,
                )
                assertThat(migreringsdata).isNotNull
                assertThat(DefaultJsonMapper.fromJson<ArenaVedtaksperiode>(migreringsdata!!.data!!).tilDato)
                    .isEqualTo(LocalDate.now().plusYears(1))
            }
    }

    @Test
    fun `skal automatisk legge inn beregningstidspunkt fra Arena i migreringsbehandlingen`() {
        unleash = MigreringFraArenaMedAutomatiskBeregningstidspunktFlytTestUnleash::class

        val (sak, migreringsbehandling) = migrerFraArena()
        val startDato = sak.rettighetsperiode.fom
        val beregningstidspunktFraArena = FakeArenaOppslagGateway()
            .hentBeregningstidspunkt("2016-123456").beregningstidspunkt

        migreringsbehandling
            .løsAvklaringsBehov(
                VurderKravLøsning(
                    kravVurderinger = setOf(
                        MigrertKravLøsningDto(
                            begrunnelse = "en begrunnelse",
                            virkningstidspunktArena = startDato.minusMonths(5),
                            muligRettFra = startDato,
                            arenaSaksnummer = "2016-123456",
                            rettighetstype = MigrertRettighetstype.ORDINÆR,
                            resterendeKvoteOrdinær = 300,
                        )
                    )
                )
            )
            .løsLovvalg(startDato, medlem = true)
            .løsSykdom(startDato, erOppfylt = true)
            .løsBistand(startDato, erOppfylt = true)
            .løsRefusjonskrav()
            .bekreftVurderinger()
            .kvalitetssikre()
            .medKontekst {
                val vurdering = repositoryProvider.provide<BeregningVurderingRepository>()
                    .hentHvisEksisterer(behandling.id)!!.tidspunktVurdering!!
                assertThat(vurdering.vurdertAv).isEqualTo(ARENA_MIGRERING_BRUKER)
                assertThat(vurdering.nedsattArbeidsevneEllerStudieevneDato).isEqualTo(beregningstidspunktFraArena)
                assertThat(vurdering.årsak).isEqualTo(ÅrsakBeregningstidspunkt.ANNET)
                assertThat(vurdering.begrunnelse).isEqualTo("Migrert fra Arena")
                assertThat(avklaringsbehovSomMåLøses.map { it.definisjon })
                    .doesNotContain(Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT)

                val migreringsdata = repositoryProvider.provide<ArenaMigreringsdataRepository>()
                    .hentAktivHvisEksisterer(behandlingId = behandling.id, steg = StegType.FASTSETT_BEREGNINGSTIDSPUNKT)
                assertThat(migreringsdata).isNotNull
                assertThat(DefaultJsonMapper.fromJson<ArenaBeregningstidspunkt>(migreringsdata!!.data!!).beregningstidspunkt)
                    .isEqualTo(beregningstidspunktFraArena)
            }
    }

    private fun migrerFraArena(
        person: TestPerson = TestPersoner.STANDARD_PERSON(),
        mottattTidspunkt: LocalDateTime = LocalDateTime.now(clock),
        saksnummerArena: String = "2016-123456",
    ): Pair<Sak, Behandling> {
        val sak = dataSource.transaction { connection ->
            PersonOgSakService(
                gatewayProvider,
                postgresRepositoryRegistry.provider(connection)
            ).opprettSakMedArenaMigrering(
                ident = person.aktivIdent(),
                søknadsdato = mottattTidspunkt.toLocalDate(),
                saksnummerArena = saksnummerArena,
            )
        }

        sak.sendInn(
            referanse = InnsendingReferanse(
                InnsendingReferanse.Type.MIGRERING_FRA_ARENA,
                UUID.randomUUID().toString(),
            ),
            type = InnsendingType.MIGRERING_FRA_ARENA,
            kanal = Kanal.DIGITAL,
            mottattTidspunkt = mottattTidspunkt,
            melding = MigreringFraArenaV0("Migrering av Arenasak $saksnummerArena"),
        )

        return hentSak(sak.saksnummer) to hentSisteOpprettedeBehandlingForSak(sak.id)
    }

}

object MigreringFraArenaMedAutomatiskSykdomFlytTestUnleash : FakeUnleashBaseWithDefaultDisabled(
    enabledFlags = listOf(
        BehandlingsflytFeature.MigererSykdomFraArenaAutomatisk,
    )
)

object MigreringFraArenaMedAutomatiskeVurderingerFlytTestUnleash : FakeUnleashBaseWithDefaultDisabled(
    enabledFlags = listOf(
        BehandlingsflytFeature.MigrererKravFraArenaAutomatisk,
        BehandlingsflytFeature.MigererSykdomFraArenaAutomatisk,
        BehandlingsflytFeature.MigrerRefusjonskravFraArenaAutomatisk,
    )
)

object MigreringFraArenaMedAutomatiskOppholdskravFlytTestUnleash : FakeUnleashBaseWithDefaultDisabled(
    enabledFlags = listOf(
        BehandlingsflytFeature.MigrererOppholdskravFraArenaAutomatisk,
    )
)

object MigreringFraArenaMedAutomatiskForutgåendeMedlemskapFlytTestUnleash : FakeUnleashBaseWithDefaultDisabled(
    enabledFlags = listOf(
        BehandlingsflytFeature.MigrererForutgaaendeMedlemskapFraArenaAutomatisk,
    )
)

object MigreringFraArenaMedAutomatiskVedtakslengdeFlytTestUnleash : FakeUnleashBaseWithDefaultDisabled(
    enabledFlags = listOf(
        BehandlingsflytFeature.MigrererVedtakslengdeFraArenaAutomatisk,
    )
)

object MigreringFraArenaMedAutomatiskBeregningstidspunktFlytTestUnleash : FakeUnleashBaseWithDefaultDisabled(
    enabledFlags = listOf(
        BehandlingsflytFeature.MigrererBeregningstidspunktFraArenaAutomatisk,
    )
)

object MigreringFraArenaMedAutomatiskKravFlytTestUnleash : FakeUnleashBaseWithDefaultDisabled(
    enabledFlags = listOf(
        BehandlingsflytFeature.MigrererKravFraArenaAutomatisk,
    )
)