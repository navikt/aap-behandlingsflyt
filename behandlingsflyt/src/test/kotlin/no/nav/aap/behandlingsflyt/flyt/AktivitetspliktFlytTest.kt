package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivBrevAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivForhåndsvarselBruddAktivitetspliktBrevLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SykdomsvurderingForBrevLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VentePåFristForhåndsvarselAktivitetsplikt11_7Løsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderBrudd11_7Løsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderBrudd11_9Løsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7LøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9LøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Brudd
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Grunn
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall as VilkårsresultatUtfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandVurderingLøsningDto
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7RepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeUnleashFasttrackAktivitetsplikt
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZoneOffset

class AktivitetspliktFlytTest :
    AbstraktFlytOrkestratorTest(FakeUnleashFasttrackAktivitetsplikt::class) {

    @Test
    fun `Happy-case flyt for aktivitetsplikt 11_7`() {
        val person = TestPersoner.STANDARD_PERSON()
        val sak = happyCaseFørstegangsbehandling(person = person)
        var åpenBehandling = revurdereFramTilOgMedSykdom(sak, sak.rettighetsperiode.fom, vissVarighet = true)

        var aktivitetspliktBehandling = dataSource.transaction { connection ->
            assertThat(
                Aktivitetsplikt11_7RepositoryImpl(connection)
                    .hentHvisEksisterer(åpenBehandling.id)
            ).isNull()

            opprettAktivitetspliktBehandling(
                Vurderingsbehov.AKTIVITETSPLIKT_11_7,
                postgresRepositoryRegistry.provider(connection),
                sak
            )
        }

        assertThat(aktivitetspliktBehandling.status()).isEqualTo(Status.OPPRETTET)

        prosesserBehandling(aktivitetspliktBehandling)

        val aktivtStegFørEffektueringsbehandling = åpenBehandling.aktivtSteg()

        val bruddFom = sak.rettighetsperiode.fom.plusWeeks(20)

        aktivitetspliktBehandling = hentBehandling(aktivitetspliktBehandling.referanse)
            .medKontekst {
                assertThat(this.behandling).extracting { it.aktivtSteg() }
                    .isEqualTo(StegType.VURDER_AKTIVITETSPLIKT_11_7)
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactlyInAnyOrder(Definisjon.VURDER_BRUDD_11_7)

            }.løsAvklaringsBehov(
                VurderBrudd11_7Løsning(
                    aktivitetsplikt11_7Vurdering = Aktivitetsplikt11_7LøsningDto(
                        begrunnelse = "Brudd",
                        erOppfylt = false,
                        utfall = Utfall.STANS,
                        gjelderFra = bruddFom
                    )
                )
            )
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactlyInAnyOrder(Definisjon.SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV)
                val brevbestillingReferanse = dataSource.transaction { connection ->
                    val aktivitetsplikt11_7Repository = Aktivitetsplikt11_7RepositoryImpl(connection)
                    aktivitetsplikt11_7Repository.hentVarselHvisEksisterer(aktivitetspliktBehandling.id)?.varselId
                        ?: error("Fant ikke varsel")

                }
                aktivitetspliktBehandling.løsAvklaringsBehov(
                    SkrivForhåndsvarselBruddAktivitetspliktBrevLøsning(
                        brevbestillingReferanse = brevbestillingReferanse.brevbestillingReferanse,
                        handling = SkrivBrevAvklaringsbehovLøsning.Handling.FERDIGSTILL,
                        behovstype = Definisjon.SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV.kode
                    )
                )
            }
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
                    .isEqualTo(Definisjon.VENTE_PÅ_FRIST_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT)
            }
            .løsAvklaringsBehov(avklaringsBehovLøsning = VentePåFristForhåndsvarselAktivitetsplikt11_7Løsning())
            .løsAvklaringsBehov(
                VurderBrudd11_7Løsning(
                    aktivitetsplikt11_7Vurdering = Aktivitetsplikt11_7LøsningDto(
                        begrunnelse = "Brudd",
                        erOppfylt = false,
                        utfall = Utfall.STANS,
                        gjelderFra = bruddFom,
                        skalIgnorereVarselFrist = true
                    )
                )
            )
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactlyInAnyOrder(Definisjon.FATTE_VEDTAK)
            }

        dataSource.transaction { connection ->
            val grunnlagIAktivitetspliktBehandling = Aktivitetsplikt11_7RepositoryImpl(connection)
                .hentHvisEksisterer(aktivitetspliktBehandling.id)
            val grunnlagIÅpenBehandling = Aktivitetsplikt11_7RepositoryImpl(connection)
                .hentHvisEksisterer(åpenBehandling.id)

            assertThat(grunnlagIAktivitetspliktBehandling).isNotNull
            assertThat(grunnlagIÅpenBehandling).isNull()
        }

        aktivitetspliktBehandling.fattVedtakEllerSendRetur().medKontekst {
            assertThat(this.behandling).extracting { it.aktivtSteg() }
                .isEqualTo(StegType.BREV)

        }.løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_11_7).medKontekst {
            assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)
        }

        var effektueringsbehandling = dataSource.transaction { connection ->
            BehandlingRepositoryImpl(connection).finnSisteOpprettedeBehandlingFor(
                sak.id,
                listOf(TypeBehandling.Revurdering)
            )
        }!!

        effektueringsbehandling = hentBehandling(effektueringsbehandling.referanse)
        assertThat(effektueringsbehandling.typeBehandling() == TypeBehandling.Revurdering)
        assertThat(effektueringsbehandling.status()).isEqualTo(Status.AVSLUTTET)
        assertThat(effektueringsbehandling.forrigeBehandlingId).isEqualTo(åpenBehandling.forrigeBehandlingId)

        åpenBehandling =
            dataSource.transaction { connection -> BehandlingRepositoryImpl(connection).hent(åpenBehandling.id) }
        assertThat(åpenBehandling.forrigeBehandlingId).isEqualTo(effektueringsbehandling.id)

        dataSource.transaction { connection ->
            val grunnlagIEffektueringsbehandling =
                Aktivitetsplikt11_7RepositoryImpl(connection).hentHvisEksisterer(effektueringsbehandling.id)
            val grunnlagIÅpenBehandling =
                Aktivitetsplikt11_7RepositoryImpl(connection).hentHvisEksisterer(åpenBehandling.id)

            assertThat(grunnlagIEffektueringsbehandling).isNotNull
            assertThat(grunnlagIÅpenBehandling).isEqualTo(grunnlagIEffektueringsbehandling)

            val underveisFørstegang =
                UnderveisRepositoryImpl(connection).hentHvisEksisterer(effektueringsbehandling.forrigeBehandlingId!!)

            val underveisÅpenEtterEffektuering = UnderveisRepositoryImpl(connection)
                .hentHvisEksisterer(åpenBehandling.id)
            assertThat(underveisFørstegang!!.perioder).isEqualTo(underveisÅpenEtterEffektuering!!.perioder)
                .describedAs {
                    "Underveissteget har ikke kjørt enda da åpen behandling er lenger bak  flyten"
                }
        }

        åpenBehandling =
            dataSource.transaction { connection -> BehandlingRepositoryImpl(connection).hent(åpenBehandling.id) }
        assertThat(åpenBehandling.aktivtSteg())
            .describedAs("Effektuering av aktivitetsplikt skal ikke endre steg for åpen behandling, dersom den åpne behandlingen står i steg før informasjonskravet")
            .isEqualTo(aktivtStegFørEffektueringsbehandling)

        åpenBehandling.løsAvklaringsBehov(
            AvklarBistandsbehovLøsning(
                bistandsVurdering = BistandVurderingLøsningDto(
                    begrunnelse = "Trenger hjelp fra nav",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = null,
                    skalVurdereAapIOvergangTilUføre = null,
                    skalVurdereAapIOvergangTilArbeid = null,
                    overgangBegrunnelse = null
                ),
            )
        )
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactlyInAnyOrder(Definisjon.SKRIV_SYKDOMSVURDERING_BREV)
            }
            .løsAvklaringsBehov(
                SykdomsvurderingForBrevLøsning(
                    vurdering = "Begrunnelse"
                ),
            )
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactlyInAnyOrder(Definisjon.FATTE_VEDTAK)
            }

        val underveisÅpenTidslinje = dataSource.transaction { connection ->
            UnderveisRepositoryImpl(connection).hent(åpenBehandling.id)
                .let { segment ->
                    Tidslinje(segment.perioder.map { Segment(it.periode, it) }).mapValue {
                        Pair(
                            it.utfall,
                            it.avslagsårsak
                        )
                    }
                }
        }

        underveisÅpenTidslinje.komprimer().assertTidslinje(
            Segment(Periode(sak.rettighetsperiode.fom, bruddFom.minusDays(1))) {
                assertEquals(VilkårsresultatUtfall.OPPFYLT, it.first)
                assertEquals(null, it.second)
            },
            Segment(Periode(bruddFom, sak.rettighetsperiode.tom)) {
                assertEquals(VilkårsresultatUtfall.IKKE_OPPFYLT, it.first)
                assertEquals(UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT_11_7_STANS, it.second)
            }
        )
    }

    @Test
    fun `Åpen behandling skal trekkes tilbake ved effktuering av aktivitetsplikt`() {
        val person = TestPersoner.STANDARD_PERSON()
        val sak = happyCaseFørstegangsbehandling(person = person)
        var åpenBehandling = revurdereFramTilOgMedSykdom(sak, sak.rettighetsperiode.fom, vissVarighet = true)

        åpenBehandling = åpenBehandling.løsAvklaringsBehov(
            AvklarBistandsbehovLøsning(
                bistandsVurdering = BistandVurderingLøsningDto(
                    begrunnelse = "Trenger hjelp fra nav",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = null,
                    skalVurdereAapIOvergangTilUføre = null,
                    skalVurdereAapIOvergangTilArbeid = null,
                    overgangBegrunnelse = null
                ),
            )
        )
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactlyInAnyOrder(Definisjon.SKRIV_SYKDOMSVURDERING_BREV)
            }
            .løsAvklaringsBehov(
                SykdomsvurderingForBrevLøsning(
                    vurdering = "Begrunnelse"
                ),
            )
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactlyInAnyOrder(Definisjon.FATTE_VEDTAK)
            }

        val aktivtStegIÅpenBehandlingFørEffektuering = åpenBehandling.aktivtSteg()

        val underveisTidslinjeFørEffekuering = dataSource.transaction { connection ->
            UnderveisRepositoryImpl(connection).hent(åpenBehandling.id)
        }.let { segment ->
            Tidslinje(segment.perioder.map { Segment(it.periode, it) }).mapValue({
                Pair(
                    it.utfall,
                    it.avslagsårsak
                )
            })
                .komprimer()
        }

        val bruddFom = sak.rettighetsperiode.fom.plusWeeks(18)
        opprettAktivitetspliktBehandlingMedVurdering(
            sak,
            Status.AVSLUTTET
        ) { behandlingId ->
            Aktivitetsplikt11_7Vurdering(
                begrunnelse = "Brudd",
                erOppfylt = false,
                utfall = Utfall.STANS,
                gjelderFra = bruddFom,
                vurdertAv = "Saksbehandler",
                opprettet = sak.rettighetsperiode.fom.plusWeeks(20).atStartOfDay().toInstant(ZoneOffset.UTC),
                vurdertIBehandling = behandlingId,
                skalIgnorereVarselFrist = false
            )
        }


        val effektueringsbehandling = dataSource.transaction { connection ->
            val repositoryProvider = postgresRepositoryRegistry.provider(connection)
            SakOgBehandlingService(repositoryProvider, gatewayProvider).finnEllerOpprettBehandling(
                sak.id,
                VurderingsbehovOgÅrsak(
                    årsak = ÅrsakTilOpprettelse.AKTIVITETSPLIKT, vurderingsbehov = listOf(
                        VurderingsbehovMedPeriode(
                            Vurderingsbehov.EFFEKTUER_AKTIVITETSPLIKT,
                            periode = sak.rettighetsperiode
                        )
                    )
                )
            )
        }

        assertThat(effektueringsbehandling is SakOgBehandlingService.MåBehandlesAtomært)
        dataSource.transaction { connection ->
            ProsesserBehandlingService(
                postgresRepositoryRegistry.provider(connection),
                gatewayProvider
            ).triggProsesserBehandling(
                effektueringsbehandling
            )
        }

        åpenBehandling =
            dataSource.transaction { connection -> BehandlingRepositoryImpl(connection).hent(åpenBehandling.id) }
        assertThat(åpenBehandling.aktivtSteg())
            .describedAs { "Skal trekkes tilbake til steget informasjonskravet står på" }
            .isEqualTo(StegType.IKKE_OPPFYLT_MELDEPLIKT)

        motor.kjørJobber()
        åpenBehandling =
            dataSource.transaction { connection -> BehandlingRepositoryImpl(connection).hent(åpenBehandling.id) }
        assertThat(åpenBehandling.aktivtSteg())
            .describedAs { "Skal prosesseres automatisk og ende opp på samme steg som før" }
            .isEqualTo(aktivtStegIÅpenBehandlingFørEffektuering)

        val underveisTidslinjeEtterEffektuering = dataSource.transaction { connection ->
            UnderveisRepositoryImpl(connection).hent(åpenBehandling.id)
        }.let { segment ->
            Tidslinje(segment.perioder.map { Segment(it.periode, it) }).mapValue {
                Pair(
                    it.utfall,
                    it.avslagsårsak
                )
            }
        }.komprimer()

        assertThat(underveisTidslinjeEtterEffektuering).isNotEqualTo(underveisTidslinjeFørEffekuering)
        underveisTidslinjeEtterEffektuering.assertTidslinje(
            Segment(Periode(sak.rettighetsperiode.fom, bruddFom.minusDays(1))) {
                assertEquals(VilkårsresultatUtfall.OPPFYLT, it.first)
                assertEquals(null, it.second)
            },
            Segment(Periode(bruddFom, sak.rettighetsperiode.tom)) {
                assertEquals(VilkårsresultatUtfall.IKKE_OPPFYLT, it.first)
                assertEquals(UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT_11_7_STANS, it.second)
            }
        )
    }

    private fun opprettAktivitetspliktBehandlingMedVurdering(
        sak: Sak,
        status: Status,
        vurdering: (behandlingId: BehandlingId) -> Aktivitetsplikt11_7Vurdering,
    ): Behandling {
        return dataSource.transaction { connection ->
            val repositoryProvider = postgresRepositoryRegistry.provider(connection)

            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val Aktivitetsplikt11_7Repository = repositoryProvider.provide<Aktivitetsplikt11_7Repository>()

            val behandling =
                opprettAktivitetspliktBehandling(Vurderingsbehov.AKTIVITETSPLIKT_11_7, repositoryProvider, sak)

            Aktivitetsplikt11_7Repository.lagre(
                behandling.id, listOf(vurdering(behandling.id))
            )
            behandlingRepository.oppdaterBehandlingStatus(behandling.id, status)
            behandling
        }
    }

    @Test
    fun `Happy-case-flyt for aktivitetsplikt § 11-9`() {
        val person = TestPersoner.STANDARD_PERSON()
        val sak = happyCaseFørstegangsbehandling(person = person)
        var åpenBehandling = revurdereFramTilOgMedSykdom(sak, sak.rettighetsperiode.fom)

        var aktivitetspliktBehandling = dataSource.transaction { connection ->
            assertThat(
                Aktivitetsplikt11_7RepositoryImpl(connection)
                    .hentHvisEksisterer(åpenBehandling.id)
            ).isNull()

            opprettAktivitetspliktBehandling(
                Vurderingsbehov.AKTIVITETSPLIKT_11_9,
                postgresRepositoryRegistry.provider(connection),
                sak
            )
        }

        assertThat(aktivitetspliktBehandling.status()).isEqualTo(Status.OPPRETTET)

        prosesserBehandling(aktivitetspliktBehandling)

        hentBehandling(aktivitetspliktBehandling.referanse)
            .medKontekst {
                assertThat(this.behandling).extracting { it.aktivtSteg() }
                    .isEqualTo(StegType.VURDER_AKTIVITETSPLIKT_11_9)

            }
            .løsAvklaringsBehov(
                VurderBrudd11_9Løsning(
                    aktivitetsplikt11_9Vurderinger = setOf(
                        Aktivitetsplikt11_9LøsningDto(
                            begrunnelse = "Det var et brudd",
                            dato = 2 januar 2020,
                            grunn = Grunn.IKKE_RIMELIG_GRUNN,
                            brudd = Brudd.IKKE_MØTT_TIL_TILTAK,
                        )
                    )
                )
            )
            .medKontekst {
                assertThat(this.behandling).extracting { it.aktivtSteg() }
                    .isEqualTo(StegType.BREV)

            }.løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_11_9).medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)
            }
    }

    private fun opprettAktivitetspliktBehandling(
        vurderingsbehov: Vurderingsbehov,
        repositoryProvider: RepositoryProvider,
        sak: Sak,
    ): Behandling {
        return SakOgBehandlingService(repositoryProvider, gatewayProvider).opprettAktivitetspliktBehandling(
            sak.id, vurderingsbehov
        )
    }

}