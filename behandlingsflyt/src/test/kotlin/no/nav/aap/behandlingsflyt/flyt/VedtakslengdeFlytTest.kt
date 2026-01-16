package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
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
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.OpprettBehandlingUtvidVedtakslengdeJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.november
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.JobbInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class VedtakslengdeFlytTest : AbstraktFlytOrkestratorTest(FakeUnleash::class) {

    @Test
    fun `forleng vedtak med passert slutt uten eksplisitt sluttdato`() {

        // Oppretter førstegangsbehandling innvilgelse ordinær, med rettighetsperiode på gammelt format (begrenset slutt)

        val gammelRettighetsperiode = Periode(1 november 2024, 1 november 2025)

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

        // Oppretter åpen revurdering for å kunne teste fletting etter atomær behandling

        var åpenBehandling = revurdereFramTilOgMedSykdom(sak, sak.rettighetsperiode.fom, vissVarighet = true)
            .løsBistand(sak.rettighetsperiode.fom)
            .løsSykdomsvurderingBrev()
            .medKontekst {
                val vedtasklengdeRepository: VedtakslengdeRepository = repositoryProvider.provide()
                val vedtakslengdeVurdering =
                    vedtasklengdeRepository.hentHvisEksisterer(this.behandling.id)
                assertThat(vedtakslengdeVurdering).isNull()
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactlyInAnyOrder(Definisjon.FATTE_VEDTAK)
            }


        // Sjekker at den åpne behandlingen har gammel rettighetsperiode og passert underveis
        dataSource.transaction { connection ->
            val underveisGrunnlag = UnderveisRepositoryImpl(connection).hentHvisEksisterer(åpenBehandling.id)
            val sisteUnderveisperiode = underveisGrunnlag?.perioder?.maxBy { it.periode.fom }!!
            assertThat(sisteUnderveisperiode.periode.tom).isEqualTo(gammelRettighetsperiode.tom)
            assertThat(sisteUnderveisperiode.utfall).isEqualTo(Utfall.OPPFYLT)
            assertThat(sisteUnderveisperiode.rettighetsType).isEqualTo(RettighetsType.BISTANDSBEHOV)

            val rettighetstypeTidslinje =
                VilkårsresultatRepositoryImpl(connection).hent(åpenBehandling.id).rettighetstypeTidslinje()
            assertThat(
                rettighetstypeTidslinje.perioder()
                    .maxOfOrNull { it.tom }).isEqualTo(gammelRettighetsperiode.tom) // Disse skal ikke ha rettighetslengde tid.maks
        }

        // Oppretter og kjører jobb for å trigge utvidelese av vedtakslengde
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

            opprettBehandlingUtvidVedtakslengdeJobbUtfører.utfør(
                JobbInput(
                    OpprettBehandlingUtvidVedtakslengdeJobbUtfører
                )
            )
        }

        motor.kjørJobber()

        // Sjekker at vi får utvidet sluttdato og rettighetsperiode i automatisk behandling
        dataSource.transaction { connection ->
            val automatiskBehandling = SakOgBehandlingService(
                postgresRepositoryRegistry.provider(connection),
                gatewayProvider
            ).finnBehandlingMedSisteFattedeVedtak(sak.id)!!

            val forventetVedtakslengdeGrunnlag =  VedtakslengdeGrunnlag(
                VedtakslengdeVurdering(
                    sluttdato = gammelRettighetsperiode.tom.plussEtÅrMedHverdager(
                        ÅrMedHverdager.ANDRE_ÅR
                    ),
                    utvidetMed = ÅrMedHverdager.ANDRE_ÅR,
                    vurdertAv = SYSTEMBRUKER,
                    vurdertIBehandling = automatiskBehandling.id,
                    opprettet = Instant.now() //Ignorer
                )
            )

            val vedtakslengdeGrunnlagAutomatisk =
                VedtakslengdeRepositoryImpl(connection).hentHvisEksisterer(automatiskBehandling.id)
            assertThat(vedtakslengdeGrunnlagAutomatisk).usingRecursiveComparison()
                .ignoringFields("vurdering.opprettet").isEqualTo(
                   forventetVedtakslengdeGrunnlag
                )


            val underveisGrunnlagAutomatisk = UnderveisRepositoryImpl(connection).hentHvisEksisterer(automatiskBehandling.id)
            val sisteUnderveisperiodeAutomatisk = underveisGrunnlagAutomatisk?.perioder?.maxBy { it.periode.fom }!!
            assertThat(sisteUnderveisperiodeAutomatisk.periode.tom).isEqualTo(forventetVedtakslengdeGrunnlag.vurdering.sluttdato)
            assertThat(sisteUnderveisperiodeAutomatisk.utfall).isEqualTo(Utfall.OPPFYLT)
            assertThat(sisteUnderveisperiodeAutomatisk.rettighetsType).isEqualTo(RettighetsType.BISTANDSBEHOV)

            val rettighetstypeTidslinjeAutomatisk =
                VilkårsresultatRepositoryImpl(connection).hent(automatiskBehandling.id).rettighetstypeTidslinje()

            // Rettighetstidslinjen begrenses av aldersvilkåret
            val aldersvilkåretAutomatisk = VilkårsresultatRepositoryImpl(connection).hent(automatiskBehandling.id)
                .finnVilkår(Vilkårtype.ALDERSVILKÅRET)

            assertThat(rettighetstypeTidslinjeAutomatisk.perioder().maxOfOrNull { it.tom }).isEqualTo(
                aldersvilkåretAutomatisk.tidslinje().segmenter().filter { it.verdi.utfall == Utfall.OPPFYLT }
                    .maxOfOrNull { it.periode.tom })

            // Sjekker at vi får utvidet sluttdato og rettighetsperiode i åpen behandling

            val vedtakslengdeGrunnlagÅpen = VedtakslengdeRepositoryImpl(connection).hentHvisEksisterer(åpenBehandling.id)
            assertThat(vedtakslengdeGrunnlagÅpen).usingRecursiveComparison().ignoringFields("vurdering.opprettet").isEqualTo(forventetVedtakslengdeGrunnlag)

            val underveisGrunnlag = UnderveisRepositoryImpl(connection).hentHvisEksisterer(åpenBehandling.id)
            val sisteUnderveisperiode = underveisGrunnlag?.perioder?.maxBy { it.periode.fom }!!
          
            // TODO: Denne feiler fordi vilkårene som er utvidet i den automatiske ikke blir flettet inn i den åpne
            assertThat(sisteUnderveisperiode.periode.tom).isEqualTo(forventetVedtakslengdeGrunnlag.vurdering.sluttdato)
            assertThat(sisteUnderveisperiode.utfall).isEqualTo(Utfall.OPPFYLT)
            assertThat(sisteUnderveisperiode.rettighetsType).isEqualTo(RettighetsType.BISTANDSBEHOV)

            val rettighetstypeTidslinje =
                VilkårsresultatRepositoryImpl(connection).hent(åpenBehandling.id).rettighetstypeTidslinje()

            // Rettighetstidslinjen begrenses av aldersvilkåret
            val aldersvilkåret = VilkårsresultatRepositoryImpl(connection).hent(åpenBehandling.id)
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