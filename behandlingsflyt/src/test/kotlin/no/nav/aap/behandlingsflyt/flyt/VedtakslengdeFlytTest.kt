package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plussEtÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.drift.Driftfunksjoner
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.OpprettBehandlingUtvidVedtakslengdeJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.november
import no.nav.aap.behandlingsflyt.test.oktober
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VedtakslengdeFlytTest : AbstraktFlytOrkestratorTest(FakeUnleash::class) {

    // TODO kan fjernes når vi ikke lenger har miljøspesifikke filter i OpprettBehandlingUtvidVedtakslengdeJobbUtfører
    @BeforeEach
    fun setup() {
        System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
    }

    @Test
    fun `forleng vedtak med passert slutt uten eksplisitt sluttdato`() {
        val gammelRettighetsperiode = Periode(1 november 2024, 30 oktober 2025)

        val (midlertidigSak, førstegangsbehandling) = sendInnFørsteSøknad()
        settRettighetsperiodeOgRekjørFraStart(midlertidigSak, gammelRettighetsperiode, førstegangsbehandling)
        val sak = hentSak(førstegangsbehandling)
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


        dataSource.transaction { connection ->
            val førstegangsbehandling = BehandlingRepositoryImpl(connection).finnFørstegangsbehandling(sak.id)!!

            val underveisGrunnlag = UnderveisRepositoryImpl(connection).hentHvisEksisterer(førstegangsbehandling.id)
            val sisteUnderveisperiode = underveisGrunnlag?.perioder?.maxBy { it.periode.fom }!!
            assertThat(sisteUnderveisperiode.periode.tom).isEqualTo(gammelRettighetsperiode.tom)
            assertThat(sisteUnderveisperiode.utfall).isEqualTo(Utfall.OPPFYLT)
            assertThat(sisteUnderveisperiode.rettighetsType).isEqualTo(RettighetsType.BISTANDSBEHOV)

            val rettighetstypeTidslinje =
                VilkårsresultatRepositoryImpl(connection).hent(førstegangsbehandling.id).rettighetstypeTidslinje()
            assertThat(
                rettighetstypeTidslinje.perioder()
                    .maxOfOrNull { it.tom }).isEqualTo(gammelRettighetsperiode.tom) // Disse skal ikke ha rettighetslengde tid.maks
        }

        dataSource.transaction { connection ->
            val repositoryProvider = postgresRepositoryRegistry.provider(connection)

            val opprettBehandlingUtvidVedtakslengdeJobbUtfører = OpprettBehandlingUtvidVedtakslengdeJobbUtfører(
                prosesserBehandlingService = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
                sakRepository = SakRepositoryImpl(connection),
                underveisRepository = UnderveisRepositoryImpl(connection),
                sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider),
                vilkårsresultatRepository = VilkårsresultatRepositoryImpl(connection),
                unleashGateway = FakeUnleash
            )

            opprettBehandlingUtvidVedtakslengdeJobbUtfører.utfør(JobbInput(OpprettBehandlingUtvidVedtakslengdeJobbUtfører))
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
                gammelRettighetsperiode.tom.plussEtÅrMedHverdager(
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

    /**
     * Denne trengs for å simulere gamle behandlinger i Kelvin som hadde 1 år med rettighetsperiode fra start
     */
    private fun settRettighetsperiodeOgRekjørFraStart(
        midlertidigSak: Sak,
        rettighetsperiode: Periode,
        behandling: Behandling
    ) {
        settRettighetsperiode(midlertidigSak, rettighetsperiode)

        // Må nullstille vilkår og rekjøre fra start
        dataSource.transaction { connection ->
            val vilkårsresultat = Vilkårsresultat()
            Vilkårtype
                .entries
                .filter { it.obligatorisk }
                .forEach { vilkårstype ->
                    vilkårsresultat
                        .leggTilHvisIkkeEksisterer(vilkårstype)
                        .leggTilIkkeVurdertPeriode(rettighetsperiode)
                }

            VilkårsresultatRepositoryImpl(connection).lagre(behandling.id, vilkårsresultat)
            Driftfunksjoner(postgresRepositoryRegistry.provider(connection), gatewayProvider).kjørFraSteg(
                behandling,
                StegType.VURDER_LOVVALG
            )
        }
    }

}