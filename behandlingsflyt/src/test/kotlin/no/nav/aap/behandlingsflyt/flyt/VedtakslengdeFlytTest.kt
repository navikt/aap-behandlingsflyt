package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plussEtÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.vedtakslengde.VedtakslengdeService
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.prosessering.OpprettJobbUtvidVedtakslengdeJobbUtfører
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.fixedClock
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.FlytJobbRepositoryImpl
import no.nav.aap.motor.JobbInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

object VedtakslengdeUnleash : FakeUnleashBaseWithDefaultDisabled(
    enabledFlags = listOf(
        BehandlingsflytFeature.UtvidVedtakslengdeJobb,
    )
)

class VedtakslengdeFlytTest : AbstraktFlytOrkestratorTest(VedtakslengdeUnleash::class) {

    private val clock = fixedClock(1 desember 2025)

    // TODO kan fjernes når vi ikke lenger har miljøspesifikke filter i OpprettJobbUtvidVedtakslengdeJobbUtfører
    @BeforeEach
    fun setup() {
        System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
    }

    @Test
    fun `forleng vedtak med passert slutt uten eksplisitt sluttdato`() {
        val søknadstidspunkt = LocalDateTime.now(clock).minusYears(1)
        val (sak, førstegangsbehandling) = sendInnFørsteSøknad(mottattTidspunkt = søknadstidspunkt)
        val rettighetsperiode = sak.rettighetsperiode
        val startDato = sak.rettighetsperiode.fom

        førstegangsbehandling
            .løsSykdom(startDato)
            .løsBistand(startDato)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt(startDato)
            .løsOppholdskrav(startDato)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_INNVILGELSE)


        val sluttdatoFørstegangsbehandling = dataSource.transaction { connection ->
            val førstegangsbehandling = BehandlingRepositoryImpl(connection).finnFørstegangsbehandling(sak.id)!!

            val underveisGrunnlag = UnderveisRepositoryImpl(connection).hentHvisEksisterer(førstegangsbehandling.id)
            val sisteUnderveisperiode = underveisGrunnlag?.perioder?.maxBy { it.periode.fom }!!
            assertThat(sisteUnderveisperiode.periode.tom).isEqualTo(rettighetsperiode.fom.plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR))
            assertThat(sisteUnderveisperiode.utfall).isEqualTo(Utfall.OPPFYLT)
            assertThat(sisteUnderveisperiode.rettighetsType).isEqualTo(RettighetsType.BISTANDSBEHOV)

            val rettighetstypeTidslinje =
                VilkårsresultatRepositoryImpl(connection).hent(førstegangsbehandling.id).rettighetstypeTidslinje()

            // Rettighetstidslinjen begrenses av aldersvilkåret
            val aldersvilkåret = VilkårsresultatRepositoryImpl(connection).hent(førstegangsbehandling.id)
                .finnVilkår(Vilkårtype.ALDERSVILKÅRET)

            assertThat(rettighetstypeTidslinje.perioder().maxOfOrNull { it.tom }).isEqualTo(
                aldersvilkåret.tidslinje().segmenter().filter { it.verdi.utfall == Utfall.OPPFYLT }
                    .maxOfOrNull { it.periode.tom })

            sisteUnderveisperiode.periode.tom
        }

        dataSource.transaction { connection ->
            val repositoryProvider = postgresRepositoryRegistry.provider(connection)

            val opprettJobbUtvidVedtakslengdeJobbUtfører = `OpprettJobbUtvidVedtakslengdeJobbUtfører`(
                sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider),
                vedtakslengdeService = VedtakslengdeService(repositoryProvider),
                flytJobbRepository = FlytJobbRepositoryImpl(connection),
                unleashGateway = VedtakslengdeUnleash,
                clock = clock,
            )

            opprettJobbUtvidVedtakslengdeJobbUtfører.utfør(JobbInput(OpprettJobbUtvidVedtakslengdeJobbUtfører))
        }

        motor.kjørJobber()

        dataSource.transaction { connection ->
            val automatiskBehandling = SakOgBehandlingService(
                postgresRepositoryRegistry.provider(connection),
                gatewayProvider
            ).finnBehandlingMedSisteFattedeVedtak(sak.id)!!

            val vedtakslengdeVurdering = VedtakslengdeRepositoryImpl(connection).hentHvisEksisterer(automatiskBehandling.id)
            assertThat(vedtakslengdeVurdering).isNotNull

            val underveisGrunnlag = UnderveisRepositoryImpl(connection).hentHvisEksisterer(automatiskBehandling.id)
            val sisteUnderveisperiode = underveisGrunnlag?.perioder?.maxBy { it.periode.fom }!!
            assertThat(sisteUnderveisperiode.periode.tom).isEqualTo(
                sluttdatoFørstegangsbehandling.plussEtÅrMedHverdager(
                    ÅrMedHverdager.ANDRE_ÅR
                )
            )
            assertThat(sisteUnderveisperiode.utfall).isEqualTo(Utfall.OPPFYLT)
            assertThat(sisteUnderveisperiode.rettighetsType).isEqualTo(RettighetsType.BISTANDSBEHOV)

            val rettighetstypeTidslinje =
                VilkårsresultatRepositoryImpl(connection).hent(automatiskBehandling.id).rettighetstypeTidslinje()

            // Rettighetstidslinjen begrenses av aldersvilkåret
            val aldersvilkåret = VilkårsresultatRepositoryImpl(connection).hent(automatiskBehandling.id)
                .finnVilkår(Vilkårtype.ALDERSVILKÅRET)

            assertThat(rettighetstypeTidslinje.perioder().maxOfOrNull { it.tom }).isEqualTo(
                aldersvilkåret.tidslinje().segmenter().filter { it.verdi.utfall == Utfall.OPPFYLT }
                    .maxOfOrNull { it.periode.tom })
        }

    }

}