package no.nav.aap.behandlingsflyt.behandling.lovvalg

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektInformasjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMåned
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.Inntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.Virksomhet
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.RelevantKravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeVurdering
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryLovvalgMedlemskapRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.behandlingsflyt.test.testGatewayProvider
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Fakes
class LovvalgMedlemskapInformasjonskravTest {

    object LovvalgInformasjonskravPå : FakeUnleashBaseWithDefaultDisabled(
        listOf(BehandlingsflytFeature.HentInnLovvalgRegisterdataForNyesteAktuelleKrav)
    )

    @Test
    fun `Skal hente inn inntekt for alle nye`() {
        val informasjonskrav = LovvalgMedlemskapInformasjonskrav.konstruer(
            inMemoryRepositoryProvider,
            testGatewayProvider(LovvalgInformasjonskravPå::class)
        )

        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 1 mars 2025)

        val kontekst = flytKontekstMedPerioder {
            this.behandling = behandling
            this.rettighetsperiode = sak.rettighetsperiode
        }

        settOppKravOgStønadsperiode(
            behandling, mapOf(
                1 mars 2025 to Pair(null, RelevantKravType.NY_STØNADSPERIODE),
                1 januar 2026 to Pair(null, RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR)

            )
        )

        val input = informasjonskrav.klargjør(kontekst)
        assertThat(input.datoerSomInntektSkalHentesIForholdTil).isEqualTo(
            setOf(1 mars 2025, 1 januar 2026)
        )
        assertThat(oppdater(informasjonskrav, input, kontekst)).isEqualTo(Informasjonskrav.Endret.ENDRET)
    }

    @Test
    fun `Skal hente inn inntekt kun for nyeste vedtatte krav dersom ingen endring i eksisterende krav`() {
        val informasjonskrav = LovvalgMedlemskapInformasjonskrav.konstruer(
            inMemoryRepositoryProvider,
            testGatewayProvider(LovvalgInformasjonskravPå::class)
        )

        val (sak, førstegangsbehandling) = opprettInMemorySakOgBehandling(søknadsdato = 1 mars 2025)
        settOppKravOgStønadsperiode(
            førstegangsbehandling, mapOf(
                1 mars 2025 to Pair(null, RelevantKravType.NY_STØNADSPERIODE),
            )
        )

        val kontekst1 = flytKontekstMedPerioder {
            this.behandling = førstegangsbehandling
            this.rettighetsperiode = sak.rettighetsperiode
        }
        val input1 = informasjonskrav.klargjør(kontekst1)
        assertThat(input1.datoerSomInntektSkalHentesIForholdTil).isEqualTo(setOf(1 mars 2025))
        assertThat(oppdater(informasjonskrav, input1, kontekst1)).isEqualTo(Informasjonskrav.Endret.ENDRET)


        val revurdering = InMemoryBehandlingRepository.opprettBehandling(
            sakId = sak.id,
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = førstegangsbehandling.id,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                årsak = ÅrsakTilOpprettelse.SØKNAD,
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD))
            )
        )

        InMemoryLovvalgMedlemskapRepository.kopier(førstegangsbehandling.id, revurdering.id)
        settOppKravOgStønadsperiode(
            revurdering, mapOf(
                1 januar 2026 to Pair(null, RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR)
            )
        )

        val kontekst2 = flytKontekstMedPerioder {
            this.behandling = revurdering
            this.rettighetsperiode = sak.rettighetsperiode
        }
        val input2 = informasjonskrav.klargjør(kontekst2)
        assertThat(input2.datoerSomInntektSkalHentesIForholdTil).isEqualTo(
            setOf(1 januar 2026)
        )
        assertThat(oppdater(informasjonskrav, input2, kontekst2)).isEqualTo(Informasjonskrav.Endret.ENDRET)


        val revurdering2 = InMemoryBehandlingRepository.opprettBehandling(
            sakId = sak.id,
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = revurdering.id,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                årsak = ÅrsakTilOpprettelse.SØKNAD,
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD))
            )
        )

        InMemoryLovvalgMedlemskapRepository.kopier(revurdering.id, revurdering2.id)
        settOppKravOgStønadsperiode(revurdering2, emptyMap())

        val kontekst3 = flytKontekstMedPerioder {
            this.behandling = revurdering2
            this.rettighetsperiode = sak.rettighetsperiode
        }

        val input3 = informasjonskrav.klargjør(kontekst3)

        assertThat(input3.datoerSomInntektSkalHentesIForholdTil).isEqualTo(
            setOf(1 januar 2026)
        )
        assertThat(oppdater(informasjonskrav, input3, kontekst3)).isEqualTo(Informasjonskrav.Endret.IKKE_ENDRET)
    }

    @Test
    fun `Skal hente inn inntekt for alle vedtatte krav som har endret dato`() {
        val informasjonskrav = LovvalgMedlemskapInformasjonskrav.konstruer(
            inMemoryRepositoryProvider,
            testGatewayProvider(LovvalgInformasjonskravPå::class)
        )

        val (sak, førstegangsbehandling) = opprettInMemorySakOgBehandling(søknadsdato = 1 mars 2025)
        val (ref1, ref2) = settOppKravOgStønadsperiode(
            førstegangsbehandling, mapOf(
                1 mars 2025 to Pair(null, RelevantKravType.NY_STØNADSPERIODE),
                1 januar 2026 to Pair(null, RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR),
            )
        )

        val revurdering = InMemoryBehandlingRepository.opprettBehandling(
            sakId = sak.id,
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = førstegangsbehandling.id,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                årsak = ÅrsakTilOpprettelse.SØKNAD,
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD))
            )
        )

        settOppKravOgStønadsperiode(
            revurdering, mapOf(
                1 februar 2025 to Pair(ref1, RelevantKravType.NY_STØNADSPERIODE),
                1 januar 2026 to Pair(ref2, RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR)
            )
        )

        val revurdering2 = InMemoryBehandlingRepository.opprettBehandling(
            sakId = sak.id,
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = revurdering.id,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                årsak = ÅrsakTilOpprettelse.SØKNAD,
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD))
            )
        )

        settOppKravOgStønadsperiode(revurdering2, emptyMap())

        val kontekst = flytKontekstMedPerioder {
            this.behandling = revurdering2
            this.rettighetsperiode = sak.rettighetsperiode
        }

        val input = informasjonskrav.klargjør(kontekst)

        assertThat(input.datoerSomInntektSkalHentesIForholdTil).isEqualTo(
            setOf(1 februar 2025, 1 januar 2026)
        )

    }

    @Test
    fun `Det skal ikke hentes inn data for gjenopptak etter stans`() {
        val informasjonskrav = LovvalgMedlemskapInformasjonskrav.konstruer(
            inMemoryRepositoryProvider,
            testGatewayProvider(LovvalgInformasjonskravPå::class)
        )

        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 1 mars 2025)

        val kontekst = flytKontekstMedPerioder {
            this.behandling = behandling
            this.rettighetsperiode = sak.rettighetsperiode
        }

        settOppKravOgStønadsperiode(
            behandling, mapOf(
                1 mars 2025 to Pair(null, RelevantKravType.NY_STØNADSPERIODE),
                1 januar 2026 to Pair(
                    null,
                    RelevantKravType.GJENOPPTAK_ETTER_STANS(listOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS))
                )
            )
        )

        val input = informasjonskrav.klargjør(kontekst)

        assertThat(input.datoerSomInntektSkalHentesIForholdTil)
            .describedAs { "Det skal ikke hentes inn data for gjenopptak etter stans" }.isEqualTo(
                setOf(1 mars 2025)
            )

        assertThat(oppdater(informasjonskrav, input, kontekst)).isEqualTo(Informasjonskrav.Endret.ENDRET)
    }


    private fun settOppKravOgStønadsperiode(
        behandling: Behandling,
        kravMap: Map<LocalDate, Pair<Kravreferanse?, RelevantKravType>>
    ): List<Kravreferanse> {
        val kravRepsitory: KravRepository = inMemoryRepositoryProvider.provide()
        val stønadsperiodeRepository: StønadsperiodeRepository = inMemoryRepositoryProvider.provide()

        val test: List<Pair<RelevantKrav, StønadsperiodeVurdering>> =
            kravMap.entries.map { (dato, referanseOgkravType) ->
                val (referanse, kravType) = referanseOgkravType
                val ref = referanse ?: Kravreferanse(UUID.randomUUID())

                Pair(
                    RelevantKrav(
                        referanse = ref,
                        journalpostId = JournalpostId("123"),
                        vurdertAv = SYSTEMBRUKER,
                        begrunnelse = "Automatisk",
                        vurdertIBehandling = behandling.id,
                        opprettet = Instant.now(),
                        søknadsdato = Søknadsdato(
                            dato,
                            årsak = SøknadsdatoÅrsak.SøknadMottatt,
                            begrunnelse = "Automatisk"
                        ),
                        overstyrMuligRettFra = null,
                        muligRettFra = dato
                    ),
                    StønadsperiodeVurdering(
                        referanse = ref,
                        startDato = dato,
                        begrunnelse = "Begrunnelse",
                        relevantKravType = kravType,
                        vurdertAv = SYSTEMBRUKER,
                        vurdertIBehandling = behandling.id,
                        opprettet = Instant.now(),
                        harHattOrdinærSiste52Uker = when (kravType) {
                            RelevantKravType.NY_STØNADSPERIODE -> false
                            RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR -> true
                            is RelevantKravType.GJENOPPTAK_ETTER_STANS -> true
                            else -> throw NotImplementedError()
                        },
                        harGjenværendeKvote = when (kravType) {
                            RelevantKravType.NY_STØNADSPERIODE -> false
                            RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR -> true
                            is RelevantKravType.GJENOPPTAK_ETTER_STANS -> true
                        }
                    )
                )
            }

        val forrigeKrav = behandling.forrigeBehandlingId?.let {
            inMemoryRepositoryProvider.provide<KravRepository>().hentHvisEksisterer(it)
        }
        val forrigeStønadsperiode = behandling.forrigeBehandlingId?.let {
            inMemoryRepositoryProvider.provide<StønadsperiodeRepository>().hentHvisEksisterer(it)
        }
        kravRepsitory.lagre(behandling.id, forrigeKrav?.vurderinger.orEmpty() + test.map { it.first }.toSet())
        stønadsperiodeRepository.lagre(
            behandling.id,
            forrigeStønadsperiode?.vurderinger.orEmpty() + test.map { it.second }.toSet()
        )

        return test.map { it.first.referanse }
    }


    private fun oppdater(
        informasjonskrav: LovvalgMedlemskapInformasjonskrav,
        input: LovvalgMedlemskapInformasjonskrav.LovvalgInput,
        kontekst: FlytKontekstMedPerioder
    ) =
        informasjonskrav.oppdater(
            input, LovvalgMedlemskapInformasjonskrav.LovvalgRegisterData(
                medlemskapPerioder = emptyList(),
                arbeidGrunnlag = emptyList(),
                inntektPerDato = input.datoerSomInntektSkalHentesIForholdTil.associate { dato ->
                    dato to listOf(
                        ArbeidsInntektMåned(
                            årMåned = YearMonth.from(dato),
                            arbeidsInntektInformasjon = ArbeidsInntektInformasjon(
                                listOf(
                                    Inntekt(
                                        beløp = 10000.0,
                                        opptjeningsland = null,
                                        skattemessigBosattLand = null,
                                        opptjeningsperiodeFom = null,
                                        opptjeningsperiodeTom = null,
                                        virksomhet = Virksomhet(
                                            identifikator = "1234"
                                        ),
                                        beskrivelse = null
                                    )
                                )
                            )
                        )
                    )
                },
                enhetGrunnlag = emptyList()
            ), kontekst
        )

}

