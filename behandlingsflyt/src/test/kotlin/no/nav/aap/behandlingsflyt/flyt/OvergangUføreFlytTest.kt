package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOvergangUføreEnkelLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOvergangUføreLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykepengerErstatningLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.flate.OvergangUføreLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.flate.OvergangUføreVurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykepengerVurderingDto
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OvergangUføreFlytTest: AbstraktFlytOrkestratorTest(AlleAvskruddUnleash::class) {
    @Test
    fun `11-18 uføre underveis i en behandling`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        val virkningsdatoFørsteLøsningOvertgangUføre = periode.fom.plusDays(2)
        val virkningsdatoAndreLøsningOvergangUføre = periode.fom.minusDays(20)

        // Sender inn en søknad
        var (_, behandling) = sendInnFørsteSøknad(periode = periode)
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
                            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null,
                            fom = periode.fom,
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
                            fom = periode.fom,
                            tom = null
                        )
                    ),
                ),
            )
            .assertThrows(
                UgyldigForespørselException::class,
                "Løsning mangler vurdering for perioder: ${
                    listOf(
                        Periode(
                            periode.fom,
                            virkningsdatoFørsteLøsningOvertgangUføre.minusDays(1)
                        )
                    )
                }"
            ) { behandling ->
                behandling.løsAvklaringsBehov(
                    AvklarOvergangUføreLøsning(
                        listOf(
                            OvergangUføreLøsningDto(
                                begrunnelse = "Løsning",
                                brukerHarSøktOmUføretrygd = true,
                                brukerHarFåttVedtakOmUføretrygd = "NEI",
                                brukerRettPåAAP = true,
                                fom = virkningsdatoFørsteLøsningOvertgangUføre,
                                tom = null,
                                overgangBegrunnelse = null
                            )
                        )
                    )
                )
            }
            .medKontekst {
                assertThat(åpneAvklaringsbehov)
                    .describedAs("Krever 11-18-løsning for perioder med 11-5 ja, 11-6 nei")
                    .anySatisfy {
                        assertThat(it.definisjon).isEqualTo(Definisjon.AVKLAR_OVERGANG_UFORE)
                    }
            }
            .løsAvklaringsBehov(
                AvklarOvergangUføreEnkelLøsning(
                    OvergangUføreVurderingLøsningDto(
                        begrunnelse = "Løsning",
                        brukerHarSøktOmUføretrygd = true,
                        brukerHarFåttVedtakOmUføretrygd = "NEI",
                        brukerRettPåAAP = true,
                        virkningsdato = virkningsdatoAndreLøsningOvergangUføre,
                        fom = virkningsdatoAndreLøsningOvergangUføre,
                        tom = null,
                        overgangBegrunnelse = null
                    )
                )
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
            .kvalitetssikreOk()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.AVKLAR_SYKEPENGEERSTATNING) }
            }
            .løsAvklaringsBehov(
                AvklarSykepengerErstatningLøsning(
                    sykepengeerstatningVurdering = SykepengerVurderingDto(
                        begrunnelse = "...",
                        dokumenterBruktIVurdering = emptyList(),
                        harRettPå = false,
                        grunn = null,
                        gjelderFra = LocalDate.now()
                    ),
                )
            )
            .løsBeregningstidspunkt()
            .løsOppholdskrav(periode.fom)
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
            dataSource.transaction { ResultatUtleder(postgresRepositoryRegistry.provider(it)).utledResultat(behandling.id) }
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
            Periode(periode.fom, virkningsdatoAndreLøsningOvergangUføre.plusMonths(8).minusDays(1)) to {
                assertThat(it.utfall).isEqualTo(Utfall.OPPFYLT)
            },
            Periode(virkningsdatoAndreLøsningOvergangUføre.plusMonths(8), underveisPeriode.tom) to {
                assertThat(it.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
            })

        resultat =
            dataSource.transaction { ResultatUtleder(postgresRepositoryRegistry.provider(it)).utledResultat(behandling.id) }

        assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)

        assertTidslinje(
            vilkårsresultat.rettighetstypeTidslinje().begrensetTil(underveisPeriode),
            Periode(periode.fom, virkningsdatoAndreLøsningOvergangUføre.plusMonths(8).minusDays(1)) to {
                assertThat(it).isEqualTo(RettighetsType.VURDERES_FOR_UFØRETRYGD)
            },
        )
    }

}