package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.flyt.AbstraktFlytOrkestratorTest
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.FeatureToggle
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import kotlin.test.Test

object manuellStyringUnleashGateway: UnleashGateway{
    var lagreStansOpphørFlagg = false



    override fun isEnabled(featureToggle: FeatureToggle): Boolean {
        if(featureToggle== BehandlingsflytFeature.LagreStansOgOpphor)
            return lagreStansOpphørFlagg
        else if (featureToggle== BehandlingsflytFeature.MigrerStansOgOpphor)
            return true
        return false
    }

    override fun isEnabled(featureToggle: FeatureToggle, ident: String): Boolean {
        return true
    }

    override fun isEnabled(featureToggle: FeatureToggle, ident: String, typeBrev: TypeBrev): Boolean {
        return true
    }
}



class StansEllerOpphørMigreringTest: AbstraktFlytOrkestratorTest(manuellStyringUnleashGateway::class) {


    @Test
    fun `kan hente sak uten stansOpphør grunnlag og og lage det`(){
        val sak = happyCaseFørstegangsbehandling()
        StansEllerOpphørMigrering(dataSource, postgresRepositoryRegistry, gatewayProvider).migrer()

        val behandling = hentSisteOpprettedeBehandlingForSak(sak.id)
        dataSource.transaction { connection ->
            val stansRepo = postgresRepositoryRegistry.provider(connection).provide<StansOpphørRepository>()
            assertThat(stansRepo.hentHvisEksisterer(behandling.id)).isNotNull

        }
    }

    @Test
    fun `migrerer ved åpen revurdering som vil gi endring`(){
        manuellStyringUnleashGateway.lagreStansOpphørFlagg = false
        val sak = happyCaseFørstegangsbehandling(LocalDate.now().minusMonths(1))
        val revurdering = revurdereFramTilOgMedSykdom(sak, LocalDate.now().minusWeeks(2))
        revurdering.løsBistand(LocalDate.now().minusWeeks(2),false)

        manuellStyringUnleashGateway.lagreStansOpphørFlagg=true
        StansEllerOpphørMigrering(dataSource, postgresRepositoryRegistry, gatewayProvider).migrer()

        dataSource.transaction { connection ->
            val stansRepo = postgresRepositoryRegistry.provider(connection).provide<StansOpphørRepository>()
            assertThat(stansRepo.hentHvisEksisterer(revurdering.forrigeBehandlingId!!)).isNotNull
            assertThat(stansRepo.hentHvisEksisterer(revurdering.id)).isNotNull
        }

        revurdering.løsSykdomsvurderingBrev().bekreftVurderinger().fattVedtak()



    }

}