package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.UføreSøknadVedtakResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingLøsningDto
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakKafkaMelding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakResultat
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakV0
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.overganguføre.OvergangUføreRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.minimalGatewayProvider
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.utils.toHumanReadable
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class OvergangUføreFlytTest : AbstraktFlytOrkestratorTest(OvergangUføreFlytTestUnleash::class) {

    @Test
    fun `11-18 uføre underveis i en behandling`() {
        val søknadstidspunkt = LocalDate.now()

        val virkningsdatoFørsteLøsningOvertgangUføre = søknadstidspunkt.plusDays(2)
        val virkningsdatoAndreLøsningOvergangUføre = søknadstidspunkt.minusDays(20)

        // Sender inn en søknad
        var (_, behandling) = sendInnFørsteSøknad(mottattTidspunkt = søknadstidspunkt.atStartOfDay())
        behandling
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsAvklaringsBehov(
                AvklarSykdomLøsning(
                    løsningerForPerioder = listOf(
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123128")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
                            yrkesskadeBegrunnelse = null,
                            fom = søknadstidspunkt,
                            tom = null
                        )
                    )
                ),
            )
            // Nei på 11-6
            .løsAvklaringsBehov(
                AvklarBistandsbehovLøsning(
                    løsningerForPerioder = listOf(
                        BistandLøsningDto(
                            begrunnelse = "Overgang uføre",
                            erBehovForAktivBehandling = false,
                            erBehovForArbeidsrettetTiltak = false,
                            erBehovForAnnenOppfølging = false,
                            skalVurdereAapIOvergangTilArbeid = null,
                            overgangBegrunnelse = "Yep",
                            fom = søknadstidspunkt,
                            tom = null
                        )
                    ),
                ),
            )
            .assertThrows(
                UgyldigForespørselException::class,
                "Du mangler vurdering for ${
                    listOf(
                        Periode(
                            søknadstidspunkt,
                            virkningsdatoFørsteLøsningOvertgangUføre.minusDays(1)
                        )
                    ).toHumanReadable()
                }"
            ) { behandling ->
                behandling.løsOvergangUføre(
                    fom = virkningsdatoFørsteLøsningOvertgangUføre,
                    tom = null,
                    brukerHarSøktOmUføretrygd = true,
                    brukerHarFåttVedtakOmUføretrygd = UføreSøknadVedtakResultat.NEI,
                    brukerHarRettPåAap = true
                )
            }
            .medKontekst {
                assertThat(åpneAvklaringsbehov)
                    .describedAs("Krever 11-18-løsning for perioder med 11-5 ja, 11-6 nei")
                    .anySatisfy {
                        assertThat(it.definisjon).isEqualTo(Definisjon.AVKLAR_OVERGANG_UFORE)
                    }
            }
            .løsOvergangUføre(
                fom = virkningsdatoAndreLøsningOvergangUføre,
                brukerHarSøktOmUføretrygd = true,
                brukerHarFåttVedtakOmUføretrygd = UføreSøknadVedtakResultat.NEI,
                brukerHarRettPåAap = true
            )
            .medKontekst {
                val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
                val overgangUføreVilkår = vilkårsresultat.finnVilkår(Vilkårtype.OVERGANGUFØREVILKÅRET)
                assertTidslinje(
                    overgangUføreVilkår.tidslinje(),
                    Periode(
                        virkningsdatoAndreLøsningOvergangUføre,
                        virkningsdatoAndreLøsningOvergangUføre.plusMonths(8).minusDays(1)
                    ) to {
                        assertThat(it.utfall).isEqualTo(Utfall.OPPFYLT)
                    },
                    // 8 måneder gjelder fra virkningsdato, selv om virkningsdato er før rettighetsperiode start
                    Periode(virkningsdatoAndreLøsningOvergangUføre.plusMonths(8), Tid.MAKS) to {
                        assertThat(it.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
                    })
            }
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(søknadstidspunkt)
            .løsAndreStatligeYtelser()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy { avklaringsbehov -> assertThat(avklaringsbehov.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .fattVedtak()
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.IVERKSETTES)
            }

        var resultat =
            dataSource.transaction {
                ResultatUtleder(
                    postgresRepositoryRegistry.provider(it),
                    minimalGatewayProvider { }).utledResultatFørstegangsBehandling(
                    behandling.id
                )
            }
        assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)

        behandling = behandling.løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_11_18)

        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
        val overgangUføreVilkår = vilkårsresultat.finnVilkår(Vilkårtype.OVERGANGUFØREVILKÅRET)
        val underveisPeriode = dataSource.transaction {
            UnderveisRepositoryImpl(it).hent(behandling.id)
        }.somTidslinje().helePerioden()

        // Sjekker at overgangUføreVilkår ble oppfylt med innvilgelsesårsak satt til 11-13.
        assertTidslinje(
            overgangUføreVilkår.tidslinje().begrensetTil(underveisPeriode),
            Periode(søknadstidspunkt, virkningsdatoAndreLøsningOvergangUføre.plusMonths(8).minusDays(1)) to {
                assertThat(it.utfall).isEqualTo(Utfall.OPPFYLT)
            })

        resultat =
            dataSource.transaction {
                ResultatUtleder(
                    postgresRepositoryRegistry.provider(it),
                    minimalGatewayProvider { }).utledResultatFørstegangsBehandling(
                    behandling.id
                )
            }

        assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)

        assertTidslinje(
            vilkårsresultat.rettighetstypeTidslinje().begrensetTil(underveisPeriode),
            Periode(søknadstidspunkt, virkningsdatoAndreLøsningOvergangUføre.plusMonths(8).minusDays(1)) to {
                assertThat(it).isEqualTo(RettighetsType.VURDERES_FOR_UFØRETRYGD)
            },
        )
    }

    @Test
    fun `11-18 uføre underveis i en behandling - deretter ikke syk`() {
        val fom = LocalDate.of(2026, 1, 1)
        val overgangUførDato = fom.plusDays(10)
        val ikkeLengerSykDato = fom.plusDays(20)

        val (sak, behandling) = sendInnFørsteSøknad(mottattTidspunkt = fom.atStartOfDay())
        behandling
            .løsSykdom(fom)
            .løsAvklaringsBehov(
                AvklarBistandsbehovLøsning(
                    løsningerForPerioder = listOf(
                        BistandLøsningDto(
                            begrunnelse = "Oppfylt bistand",
                            erBehovForAktivBehandling = true,
                            erBehovForArbeidsrettetTiltak = false,
                            erBehovForAnnenOppfølging = null,
                            skalVurdereAapIOvergangTilArbeid = null,
                            overgangBegrunnelse = "Yep",
                            fom = fom,
                            tom = null
                        ),
                        BistandLøsningDto(
                            begrunnelse = "Ikke oppfylt bistand",
                            erBehovForAktivBehandling = false,
                            erBehovForArbeidsrettetTiltak = false,
                            erBehovForAnnenOppfølging = false,
                            skalVurdereAapIOvergangTilArbeid = null,
                            overgangBegrunnelse = "Yep",
                            fom = overgangUførDato,
                            tom = null
                        )
                    ),
                ),
            )
            .løsOvergangUføre(
                fom = overgangUførDato,
                brukerHarSøktOmUføretrygd = true,
                brukerHarFåttVedtakOmUføretrygd = UføreSøknadVedtakResultat.NEI,
                brukerHarRettPåAap = true
            )
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()

        val (melding, revurdering) = opprettUførevedtakshendelse(sak, behandling)
        assertThat(revurdering.årsakTilOpprettelse).isEqualTo(ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA)

        dataSource.transaction { connection ->
            val vurderingsbehovOgÅrsaker =
                BehandlingRepositoryImpl(connection).hentVurderingsbehovOgÅrsaker(revurdering.id)
            assertThat(vurderingsbehovOgÅrsaker).hasSize(1)
            assertThat(vurderingsbehovOgÅrsaker.first().beskrivelse).isEqualTo(melding.beskrivelseVurderingsbehov())
            assertThat(vurderingsbehovOgÅrsaker.first().vurderingsbehov).hasSize(1)
            assertThat(vurderingsbehovOgÅrsaker.first().vurderingsbehov.first().type).isEqualTo(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND)
        }

        revurdering
            .løsAvklaringsBehov(
                AvklarSykdomLøsning(
                    løsningerForPerioder = listOf(
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "ok",
                            fom = ikkeLengerSykDato,
                            tom = null,
                            dokumenterBruktIVurdering = emptyList(),
                            harNedsattArbeidsevne = ArbeidsevneNedsattValg.NEI,
                            harSkadeSykdomEllerLyte = false,
                            erSkadeSykdomEllerLyteVesentligdel = null,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = null,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            yrkesskadeBegrunnelse = null,
                            kodeverk = null,
                            hoveddiagnose = null,
                            bidiagnoser = null,
                        )
                    ),
                )
            )
            .løsAvklaringsBehov(
                AvklarBistandsbehovLøsning(løsningerForPerioder = listOf())
            )
            .løsOvergangUføre(
                fom = ikkeLengerSykDato,
                brukerHarSøktOmUføretrygd = true,
                brukerHarFåttVedtakOmUføretrygd = UføreSøknadVedtakResultat.NEI,
                brukerHarRettPåAap = false
            )
            .medKontekst {
                assertThat(åpneAvklaringsbehov).hasSize(1)
                assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.AVKLAR_OVERGANG_ARBEID)
            }
            .løsOvergangArbeid(utfall = Utfall.IKKE_OPPFYLT, fom = ikkeLengerSykDato)
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).hasSize(1)
                assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.FATTE_VEDTAK)
            }
    }

    @Test
    fun `For sak med totalt avslag skal ikke uførehendelse opprette revurdering`() {
        val fom = LocalDate.of(2026, 1, 1)
        val (sak, behandling) = sendInnFørsteSøknad(mottattTidspunkt = fom.atStartOfDay())
        behandling
            .løsSykdom(fom, erOppfylt = false)
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .fattVedtak()

        val (_, revurdering) = opprettUførevedtakshendelse(sak, behandling)
        assertThat(revurdering.årsakTilOpprettelse).isNotEqualTo(ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA)
        assertThat(revurdering.id).isEqualTo(behandling.id)
    }

    @Test
    fun `innvilget uførevedtak fram i tid lagrer automatisk 11-18 vurdering uten kvalitetssikring`() {
        val fom = LocalDate.now().minusMonths(2)
        val (sak, sisteBehandling) = sendInnFørsteSøknad(mottattTidspunkt = fom.atStartOfDay())
        val virkningsdato = LocalDate.now().plusMonths(1)

        sisteBehandling
            .løsSykdom(fom, erOppfylt = false)
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .fattVedtak()

        val (_, revurdering) = opprettUførevedtakshendelse(
            sak = sak,
            behandling = sisteBehandling,
            virkningsdato = virkningsdato,
            resultat = UførevedtakResultat.INNV,
            avslag12_5 = false,
        )

        assertThat(hentAlleAvklaringsbehov(revurdering).map { it.definisjon }).doesNotContain(Definisjon.KVALITETSSIKRING)

        dataSource.transaction { connection ->
            val vurderingsbehovOgÅrsaker =
                BehandlingRepositoryImpl(connection).hentVurderingsbehovOgÅrsaker(revurdering.id)
            assertThat(vurderingsbehovOgÅrsaker.flatMap { it.vurderingsbehov.map { behov -> behov.type } })
                .contains(Vurderingsbehov.OVERGANG_UFORE_AUTOMATISK_STANS)

            val vurdering = OvergangUføreRepositoryImpl(connection)
                .hentHvisEksisterer(revurdering.id)
                ?.vurderinger
                ?.singleOrNull { it.vurdertAv == SYSTEMBRUKER.ident && it.fom == virkningsdato }

            assertThat(vurdering!!.begrunnelse).isEqualTo("Automatisk opphør på grunn av vedtak om uføre")
            assertThat(vurdering.vurdertAv).isEqualTo(SYSTEMBRUKER.ident)
            assertThat(vurdering.fom).isEqualTo(virkningsdato)
            assertThat(vurdering.brukerRettPåAAP).isFalse()
            assertThat(vurdering.brukerHarFåttVedtakOmUføretrygd).isEqualTo(UføreSøknadVedtakResultat.JA_INNVILGET_GRADERT)

            val vilkårsresultat = VilkårsresultatRepositoryImpl(connection)
                .hent(revurdering.id)

            val automatiskVurdering = OvergangUføreRepositoryImpl(connection)
                .hentHvisEksisterer(revurdering.id)
                ?.vurderinger
                ?.singleOrNull { it.vurdertAv == SYSTEMBRUKER.ident && it.fom == virkningsdato }
            
            assertThat(automatiskVurdering!!.begrunnelse).isEqualTo("Automatisk opphør på grunn av vedtak om uføre")
            assertThat(automatiskVurdering.fom).isEqualTo(virkningsdato)

            val overgangUføreVilkårFør = vilkårsresultat
                .finnVilkår(Vilkårtype.OVERGANGUFØREVILKÅRET)
                .tidslinje()
                .segment(virkningsdato.minusDays(1))
                ?.verdi

            assertThat(overgangUføreVilkårFør!!.utfall).isNotEqualTo(Utfall.IKKE_OPPFYLT)
                .withFailMessage("Periode før virkningsdato skal ikke være IKKE_OPPFYLT enda")

            val overgangUføreVilkårEtter = vilkårsresultat
                .finnVilkår(Vilkårtype.OVERGANGUFØREVILKÅRET)
                .tidslinje()
                .segment(virkningsdato)
                ?.verdi
            assertThat(overgangUføreVilkårEtter!!.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
            assertThat(overgangUføreVilkårEtter.avslagsårsak).isEqualTo(Avslagsårsak.IKKE_RETT_PA_AAP_UNDER_BEHANDLING_AV_UFORE)
        }
    }

    @Test
    fun `innvilget uførevedtak fram i tid bruker uføregateway for å utlede full innvilgelse`() {
        val fom = LocalDate.now().minusMonths(2)
        val virkningsdato = LocalDate.now().plusMonths(1)
        val person = TestPersoner.STANDARD_PERSON().medUføre(
            uføre = Prosent(100),
            virkningstidspunkt = virkningsdato,
        )
        val (sak, sisteBehandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
        )

        sisteBehandling
            .løsSykdom(fom, erOppfylt = false)
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .fattVedtak()

        val (_, revurdering) = opprettUførevedtakshendelse(
            sak = sak,
            behandling = sisteBehandling,
            virkningsdato = virkningsdato,
            resultat = UførevedtakResultat.INNV,
            avslag12_5 = false,
        )

        dataSource.transaction { connection ->
            val vurdering = OvergangUføreRepositoryImpl(connection)
                .hentHvisEksisterer(revurdering.id)
                ?.vurderinger
                ?.singleOrNull { it.vurdertAv == SYSTEMBRUKER.ident && it.fom == virkningsdato }
            assertThat(vurdering).isNotNull
            assertThat(vurdering!!.brukerHarFåttVedtakOmUføretrygd).isEqualTo(UføreSøknadVedtakResultat.JA_INNVILGET_FULL)
        }
    }

    private fun opprettUførevedtakshendelse(
        sak: Sak,
        behandling: Behandling,
        virkningsdato: LocalDate = LocalDate.now(),
        resultat: UførevedtakResultat = UførevedtakResultat.AVSL,
        avslag12_5: Boolean = true,
    ): Pair<UførevedtakV0, Behandling> {
        val dokumentReferanse = UUID.randomUUID().toString()
        val melding = UførevedtakKafkaMelding(
            personId = sak.person.aktivIdent().toString(),
            virkningsdato = virkningsdato,
            resultat = resultat,
            avslag12_5 = avslag12_5,
        ).tilUføreVedtakV0() as UførevedtakV0
        dataSource.transaction { connection ->
            val flytJobbRepository = FlytJobbRepository(connection)
            flytJobbRepository.leggTil(
                HendelseMottattHåndteringJobbUtfører.nyJobb(
                    sakId = behandling.sakId,
                    dokumentReferanse = InnsendingReferanse(
                        InnsendingReferanse.Type.UFØREVEDTAK_HENDELSE_ID,
                        dokumentReferanse
                    ),
                    brevkategori = InnsendingType.UFØRE_VEDTAK_HENDELSE,
                    kanal = Kanal.DIGITAL,
                    mottattTidspunkt = LocalDateTime.now(),
                    melding = melding
                )
            )
        }
        motor.kjørJobber()

        var revurdering = hentSisteOpprettedeBehandlingForSak(sak.id)
        if (!revurdering.status().erAvsluttet()) {
            revurdering = prosesserBehandling(revurdering)
        }
        return Pair(melding, revurdering)
    }

    @Test
    fun `Legge til revurdering av 11-18`() {
        /*
        Legg til "§ 11-18 AAP under behandling av krav om uføretrygd" som revurderingsårsak
        1. Hvis det finnes en eksisterende 11-18 vurdering, så skal det trigge avklaringsbehov på 11-18
        2. Hvis det ikke finnes vurdert 11-18, så skal det trigge en vurdering av § 11-6 + 11-18.
         */
        val startDato = LocalDate.now()
        val uføreDato = startDato.minusYears(3)
        val sak = happyCaseFørstegangsbehandling(
            fom = startDato, sendMeldekort = false, person = TestPersoner.STANDARD_PERSON().medUføre(
                virkningstidspunkt = uføreDato,
                uføre = Prosent(50)
            )
        )

        val overgangUføreDato = startDato.plusDays(8)
        /* Gir AAP som arbeidssøker. */
        var revurdering = sak.opprettManuellRevurdering(
            no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.OVERGANG_UFORE
        )

        if (hentAlleAvklaringsbehov(revurdering).any { it.definisjon == Definisjon.AVKLAR_BISTANDSBEHOV }) {
            revurdering = revurdering.løsAvklaringsBehov(
                AvklarBistandsbehovLøsning(
                    løsningerForPerioder = listOf(
                        BistandLøsningDto(
                            begrunnelse = "Trenger hjelp fra nav",
                            erBehovForAktivBehandling = true,
                            erBehovForArbeidsrettetTiltak = false,
                            erBehovForAnnenOppfølging = null,
                            skalVurdereAapIOvergangTilArbeid = null,
                            overgangBegrunnelse = null,
                            fom = startDato,
                            tom = startDato.plusDays(7)
                        ),
                        BistandLøsningDto(
                            fom = overgangUføreDato,
                            begrunnelse = "Trenger hjelp fra nav",
                            erBehovForAktivBehandling = false,
                            erBehovForArbeidsrettetTiltak = false,
                            erBehovForAnnenOppfølging = false,
                            skalVurdereAapIOvergangTilArbeid = null,
                            overgangBegrunnelse = "Skal over i arbeid",
                            tom = null
                        ),
                    )
                )
            )
        }

        if (hentAlleAvklaringsbehov(revurdering).none { it.definisjon == Definisjon.AVKLAR_OVERGANG_UFORE }) {
            revurdering = prosesserBehandling(revurdering)
        }

        if (hentAlleAvklaringsbehov(revurdering).any { it.definisjon == Definisjon.AVKLAR_OVERGANG_UFORE }) {
            revurdering = revurdering.løsOvergangUføre(
                fom = overgangUføreDato,
                brukerHarSøktOmUføretrygd = true,
                brukerHarFåttVedtakOmUføretrygd = UføreSøknadVedtakResultat.NEI,
                brukerHarRettPåAap = true
            )
        }

        revurdering.medKontekst {
            assertThat(åpneAvklaringsbehov.map { it.definisjon }).doesNotContain(Definisjon.AVKLAR_OVERGANG_UFORE)
        }

        if (hentAlleAvklaringsbehov(revurdering).any { it.definisjon == Definisjon.AVKLAR_OVERGANG_ARBEID }) {
            revurdering
                // Denne skal ideelt ikke løftes, men ikke et veldig vanlig case (delvis ufør + ja 11-18)
                .løsOvergangArbeid(utfall = Utfall.IKKE_OPPFYLT, fom = overgangUføreDato.plusMonths(8))
                .løsSykdomsvurderingBrev()
                .bekreftVurderinger()
                .fattVedtak()
        }

        sak.opprettManuellRevurdering(
            no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.OVERGANG_UFORE
        )
            .medKontekst {
                assertThat(behandling.aktivtSteg()).isIn(StegType.START_BEHANDLING, StegType.OVERGANG_UFORE)
            }
    }
}

object OvergangUføreFlytTestUnleash : FakeUnleashBaseWithDefaultDisabled(
    enabledFlags = listOf(
        BehandlingsflytFeature.IngenValidering,
        BehandlingsflytFeature.AutomatiskStans1118,
    )
)
