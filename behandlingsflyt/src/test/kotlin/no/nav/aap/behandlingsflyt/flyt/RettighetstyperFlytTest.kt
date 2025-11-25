package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykepengerErstatningLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plusHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plussEtÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerGrunn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykepengerVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RettighetstyperFlytTest : AbstraktFlytOrkestratorTest(FakeUnleash::class) {

    @Test
    fun `Sykepengeerstatning med yrkesskade`() {
        val fom = 1 april 2025
        val person = TestPersoner.PERSON_MED_YRKESSKADE()
        val periode = Periode(fom, fom.plusYears(3))
        var (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
            periode = periode,
        )

        behandling = behandling.medKontekst {
            assertThat(åpneAvklaringsbehov).isNotEmpty()
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }
            .løsSykdom(sak.rettighetsperiode.fom, vissVarighet = false, erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true)
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).describedAs(
                    "Bistandsbehov skal ikke vurderes hvis viss varighet er nei"
                ).doesNotContain(Definisjon.AVKLAR_BISTANDSBEHOV)
            }
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).describedAs(
                    "Person med yrkesskade skal gi avklaringsbehov for yrkesskade"
                ).containsExactly(Definisjon.AVKLAR_YRKESSKADE)
            }
            .løsYrkesskade(person)
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).describedAs(
                    "Viss varighet false skal gi avklaringsbehov for sykepengeerstatning"
                ).containsExactly(Definisjon.AVKLAR_SYKEPENGEERSTATNING)
            }
            .løsAvklaringsBehov(
                AvklarSykepengerErstatningLøsning(
                    sykepengeerstatningVurdering = SykepengerVurderingDto(
                        begrunnelse = "...",
                        dokumenterBruktIVurdering = emptyList(),
                        harRettPå = true,
                        grunn = SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR,
                        gjelderFra = periode.fom
                    ),
                )
            )
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).describedAs(
                    "Forutgående medlemskap skal ikke vurderes for yrkesskade"
                ).doesNotContain(Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP)
            }
            .løsBeregningstidspunkt(periode.fom)
            .løsYrkesskadeInntekt(person.yrkesskade)
            .løsOppholdskrav(periode.fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .assertRettighetstype(
                Periode(
                    sak.rettighetsperiode.fom,
                    sak.rettighetsperiode.fom.plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR) // TODO: Rettighetstypen skal vare et halvt år
                ) to RettighetsType.SYKEPENGEERSTATNING,
            )
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.IVERKSETTES)

                val resultat = dataSource.transaction {
                    ResultatUtleder(postgresRepositoryRegistry.provider(it)).utledResultat(behandling.id)
                }
                assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)
            }

    }
}