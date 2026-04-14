package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.AbstraktFlytOrkestratorTest
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.FeatureToggle
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
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
                assertThat(vurdering.erNedsettelseMerEnnYrkesskadegrense).isNull()
            }
        }
    }

    @Test
    fun `kan migrere sykdomsvurdering med vurdert dato før rettighetsperioden utledede felter`() {
        val søknadsdato = LocalDate.of(2026, 4, 1)
        val vurdertFra = LocalDate.of(2026, 3, 1)
        val (sak, behandling) = sendInnFørsteSøknad(
            mottattTidspunkt = søknadsdato.atStartOfDay(),
        )
        behandling
            .løsSykdom(vurdertFra)
            .løsBistand(vurdertFra)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(søknadsdato)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()


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
                assertThat(vurdering.erNedsettelseMerEnnYrkesskadegrense).isNull()
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

    @Test
    fun `migrering av sykdomsvilkår med oppfylt ordinær selv om bistand ikke trengs skal gå bra`() {
        val søknadsdato = LocalDate.of(2026, 1, 1)
        val (sak, behandling) = sendInnFørsteSøknad(
            mottattTidspunkt = søknadsdato.atStartOfDay(),
        )
        behandling
            .løsSykdom(søknadsdato)
            .løsBistand(søknadsdato, erOppfylt = false)
            .løsOvergangUføre(søknadsdato)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsSykepengeerstatning(søknadsdato to false)
            .foreslåVedtak()
            .fattVedtak()


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

        // Manuelt hack vilkårsvurdering slik at den blir lik som pre 2025-12-01
        dataSource.transaction { connection ->
            val vilkårsresultatRepository = VilkårsresultatRepositoryImpl(connection)
            val vilkårsresultat = vilkårsresultatRepository.hent(behandling.id)
            val sykdomsVilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
            val førsteVurdering = sykdomsVilkår.vilkårsperioder().first()
            sykdomsVilkår.leggTilVurdering(
                Vilkårsperiode(
                    periode = førsteVurdering.periode,
                    utfall = Utfall.OPPFYLT,
                    manuellVurdering = førsteVurdering.manuellVurdering,
                    begrunnelse = førsteVurdering.begrunnelse,
                    innvilgelsesårsak = null,
                    avslagsårsak = null,
                    faktagrunnlag = førsteVurdering.faktagrunnlag,
                    versjon = førsteVurdering.versjon
                )
            )
            vilkårsresultatRepository.lagre(behandling.id, vilkårsresultat)
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
                assertThat(vurdering.erNedsettelseMerEnnYrkesskadegrense).isNull()
            }
        }
    }
}


