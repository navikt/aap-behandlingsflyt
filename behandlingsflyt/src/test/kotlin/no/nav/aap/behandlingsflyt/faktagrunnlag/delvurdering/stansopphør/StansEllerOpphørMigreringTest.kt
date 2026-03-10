package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.flyt.AbstraktFlytOrkestratorTest
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.FeatureToggle
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import kotlin.test.Test

object ManuellStyringUnleashGateway : UnleashGateway {
    var lagreStansOpphørFlagg = false

    override fun isEnabled(featureToggle: FeatureToggle) = when (featureToggle) {
        BehandlingsflytFeature.LagreStansOgOpphor -> lagreStansOpphørFlagg
        BehandlingsflytFeature.MigrerStansOgOpphor -> true
        else -> false
    }
    override fun isEnabled(featureToggle: FeatureToggle, ident: String) = true
    override fun isEnabled(featureToggle: FeatureToggle, ident: String, typeBrev: TypeBrev) = true
}

class StansEllerOpphørMigreringTest : AbstraktFlytOrkestratorTest(ManuellStyringUnleashGateway::class) {
    @Test
    fun `kan hente sak uten stansOpphør grunnlag og og lage det`() {
        ManuellStyringUnleashGateway.lagreStansOpphørFlagg = false
        val sak = happyCaseFørstegangsbehandling()
        StansEllerOpphørMigrering(dataSource, postgresRepositoryRegistry, gatewayProvider).migrer()

        val behandling = hentSisteOpprettedeBehandlingForSak(sak.id)
        dataSource.transaction { connection ->
            val stansRepo = postgresRepositoryRegistry.provider(connection).provide<StansOpphørRepository>()
            assertThat(stansRepo.hentHvisEksisterer(behandling.id)).isNotNull
        }
    }

    @Test
    fun `migrerer ved åpen revurdering som vil gi endring`() {
        ManuellStyringUnleashGateway.lagreStansOpphørFlagg = false
        val søknadsdato = LocalDate.now().minusMonths(1)
        val sak = happyCaseFørstegangsbehandling(søknadsdato)

        val revurdererFra = LocalDate.now().minusWeeks(2)
        val revurdering = revurdereFramTilOgMedSykdom(sak, revurdererFra)
            .løsBistand(revurdererFra, false)

        ManuellStyringUnleashGateway.lagreStansOpphørFlagg = true
        StansEllerOpphørMigrering(dataSource, postgresRepositoryRegistry, gatewayProvider).migrer()

        val behandlingIdFørstegangsbehandling = revurdering.forrigeBehandlingId!!

        dataSource.transaction { connection ->
            val stansOpphørRepository = postgresRepositoryRegistry.provider(connection).provide<StansOpphørRepository>()

            val stansOpphørFørstegangsbehandling = stansOpphørRepository.hentHvisEksisterer(behandlingIdFørstegangsbehandling)!!
            assertThat(stansOpphørFørstegangsbehandling.stansOgOpphør).hasSize(1)
            stansOpphørFørstegangsbehandling.stansOgOpphør.single().also {
                assertThat(it.vurdertIBehandling).isEqualTo(behandlingIdFørstegangsbehandling)
                /* Ca 3 år frem i tid. */
                assertThat(it.fom).isBetween(søknadsdato.plusYears(3).minusMonths(1), søknadsdato.plusYears(3).plusMonths(1))
                it as GjeldendeStansEllerOpphør
                assertThat(it.vurdering.årsaker).containsExactly(Avslagsårsak.ORDINÆRKVOTE_BRUKT_OPP)
            }
        }

        revurdering
            .løsOvergangUføre(revurdererFra)
            .løsOvergangArbeid(Utfall.IKKE_OPPFYLT, fom = revurdererFra)
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .løsSykepengeerstatning(revurdererFra to false)
            .løsForeslåVedtak()
            .fattVedtak()

        dataSource.transaction { connection ->
            val stansOpphørRepository = postgresRepositoryRegistry.provider(connection).provide<StansOpphørRepository>()
            val stansOpphørRevurdering = stansOpphørRepository.hentHvisEksisterer(revurdering.id)!!

            assertThat(stansOpphørRevurdering.stansOgOpphør).hasSize(3)
            assertThat(stansOpphørRevurdering.stansOgOpphør.filter { it.vurdertIBehandling == revurdering.id })
                .hasSize(2)

            assertThat(stansOpphørRevurdering.gjeldendeStansOgOpphør()).hasSize(1)
            val gjeldende = stansOpphørRevurdering.gjeldendeStansOgOpphør().single()
            assertThat(gjeldende.fom).isEqualTo(revurdererFra)
            assertThat(gjeldende.vurdertIBehandling).isEqualTo(revurdering.id)
            assertThat(gjeldende.vurdering.årsaker).containsExactly(Avslagsårsak.IKKE_BEHOV_FOR_OPPFOLGING)
        }
    }
}