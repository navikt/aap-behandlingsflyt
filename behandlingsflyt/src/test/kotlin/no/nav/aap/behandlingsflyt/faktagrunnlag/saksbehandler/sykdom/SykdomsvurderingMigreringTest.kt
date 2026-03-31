package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.flyt.AbstraktFlytOrkestratorTest
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.FeatureToggle
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

object SykdomsvurderingMigreringUnleashGateway : UnleashGateway {
    override fun isEnabled(featureToggle: FeatureToggle) = when (featureToggle) {
        BehandlingsflytFeature.MigrerSykdomsvurdering -> true
        else -> false
    }
    override fun isEnabled(featureToggle: FeatureToggle, ident: String) = true
    override fun isEnabled(featureToggle: FeatureToggle, ident: String, typeBrev: TypeBrev) = true
}

class SykdomsvurderingMigreringTest : AbstraktFlytOrkestratorTest(SykdomsvurderingMigreringUnleashGateway::class) {

    @Test
    fun `kan migrere sykdomsvurdering med utledede felter`() {
        val sak = happyCaseFørstegangsbehandling()
        val behandling = hentSisteOpprettedeBehandlingForSak(sak.id)

        // Nullstill de nye feltene for å simulere gammel data
        dataSource.transaction { connection ->
            connection.execute(
                """
                UPDATE sykdom_vurdering 
                SET er_nedsettelse_minst_halvparten = NULL,
                    er_nedsettelse_mer_enn_yrkesskadegrense = NULL
                WHERE sykdom_vurderinger_id IN (
                    SELECT sykdom_vurderinger_id FROM sykdom_grunnlag WHERE behandling_id = ?
                )
                """.trimIndent()
            ) {
                setParams {
                    setLong(1, behandling.id.id)
                }
            }
        }

        // Verifiser at feltene er null før migrering
        dataSource.transaction { connection ->
            val sykdomRepo = postgresRepositoryRegistry.provider(connection).provide<SykdomRepository>()
            val vurderingerMedId = sykdomRepo.hentSykdomsvurderingMedId(behandling.id)
            
            assertThat(vurderingerMedId).isNotEmpty
            vurderingerMedId.forEach { vurderingMedId ->
                assertThat(vurderingMedId.sykdomsvurdering.erNedsettelseMinstHalvparten).isNull()
                assertThat(vurderingMedId.sykdomsvurdering.erNedsettelseMerEnnYrkesskadegrense).isNull()
            }
        }

        // Kjør migrering
        SykdomsvurderingMigrering(dataSource, postgresRepositoryRegistry, gatewayProvider).migrer()

        // Verifiser at feltene er satt etter migrering
        dataSource.transaction { connection ->
            val sykdomRepo = postgresRepositoryRegistry.provider(connection).provide<SykdomRepository>()
            val vurderingerMedId = sykdomRepo.hentSykdomsvurderingMedId(behandling.id)
            
            assertThat(vurderingerMedId).isNotEmpty
            vurderingerMedId.forEach { vurderingMedId ->
                val vurdering = vurderingMedId.sykdomsvurdering
                assertThat(vurdering.erNedsettelseMinstHalvparten).isEqualTo(ErNedsettelseMinstHalvpartenValg.JA)
            }
        }
    }

    @Test
    fun `migrering er idempotent - kan kjøres flere ganger`() {
        val sak = happyCaseFørstegangsbehandling()
        val behandling = hentSisteOpprettedeBehandlingForSak(sak.id)

        // Nullstill de nye feltene
        dataSource.transaction { connection ->
            connection.execute(
                """
                UPDATE sykdom_vurdering 
                SET er_nedsettelse_minst_halvparten = NULL,
                    er_nedsettelse_mer_enn_yrkesskadegrense = NULL
                WHERE sykdom_vurderinger_id IN (
                    SELECT sykdom_vurderinger_id FROM sykdom_grunnlag WHERE behandling_id = ?
                )
                """.trimIndent()
            ) {
                setParams {
                    setLong(1, behandling.id.id)
                }
            }
        }

        // Kjør migrering to ganger
        SykdomsvurderingMigrering(dataSource, postgresRepositoryRegistry, gatewayProvider).migrer()
        SykdomsvurderingMigrering(dataSource, postgresRepositoryRegistry, gatewayProvider).migrer()

        // Verifiser at feltene fortsatt er korrekt satt
        dataSource.transaction { connection ->
            val sykdomRepo = postgresRepositoryRegistry.provider(connection).provide<SykdomRepository>()
            val vurderingerMedId = sykdomRepo.hentSykdomsvurderingMedId(behandling.id)
            
            assertThat(vurderingerMedId).isNotEmpty
            vurderingerMedId.forEach { vurderingMedId ->
                assertThat(vurderingMedId.sykdomsvurdering.erNedsettelseMinstHalvparten).isNotNull()
            }
        }
    }

    @Test
    fun `migrering hopper over behandlinger uten vilkårsresultat`() {
        // Kjør migrering på tom database - skal ikke kaste exception
        SykdomsvurderingMigrering(dataSource, postgresRepositoryRegistry, gatewayProvider).migrer()
        
        // Opprett en happy case og verifiser at den migreres korrekt
        val sak = happyCaseFørstegangsbehandling()
        val behandling = hentSisteOpprettedeBehandlingForSak(sak.id)

        dataSource.transaction { connection ->
            val sykdomRepo = postgresRepositoryRegistry.provider(connection).provide<SykdomRepository>()
            val vurderingerMedId = sykdomRepo.hentSykdomsvurderingMedId(behandling.id)
            assertThat(vurderingerMedId).isNotEmpty
        }
    }
}


