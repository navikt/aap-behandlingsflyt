package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårsresultatRepositoryImplTest {
    @Test
    fun `Test oprett vilkårsresultat og hent ut igjen`() {
        val dataSource = InitTestDatabase.freshDatabase()

        val behandling = dataSource.transaction { connection ->
            // Opprett person, sak og behandling
            val personOgSakService = PersonOgSakService(
                FakePdlGateway,
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            )
            val behandlingRepo = BehandlingRepositoryImpl(connection)

            val sak =
                personOgSakService.finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
            val behandling =
                behandlingRepo.opprettBehandling(
                    sak.id,
                    TypeBehandling.Førstegangsbehandling,
                    null,
                    VurderingsbehovOgÅrsak(
                        listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                        ÅrsakTilOpprettelse.SØKNAD
                    )
                )
            behandling
        }

        // Opprett vilkårsresultat
        val søknadsdato = LocalDate.now().minusDays(29)
        val resultat = Vilkårsresultat(
            null,
            listOf(
                Vilkår(
                    Vilkårtype.SYKDOMSVILKÅRET,
                    setOf(
                        Vilkårsperiode(
                            Periode(
                                søknadsdato.plusYears(3).minusMonths(4).plusDays(1),
                                søknadsdato.plusYears(3)
                            ),
                            Utfall.IKKE_OPPFYLT,
                            false,
                            null,
                            avslagsårsak = Avslagsårsak.IKKE_SYKDOM_SKADE_LYTE_VESENTLIGDEL,
                            faktagrunnlag = null
                        )
                    )
                ),
                Vilkår(
                    Vilkårtype.BISTANDSVILKÅRET,
                    setOf(
                        Vilkårsperiode(
                            periode = Periode(
                                søknadsdato.plusYears(1).minusMonths(4).plusDays(1),
                                søknadsdato.plusYears(6)
                            ),
                            utfall = Utfall.OPPFYLT,
                            manuellVurdering = false,
                            begrunnelse = null,
                            faktagrunnlag = null
                        )
                    )
                ),
                Vilkår(
                    Vilkårtype.SAMORDNING,
                    emptySet()
                )
            )
        )
        dataSource.transaction { connection ->
            val vilkårsResultatRepo = VilkårsresultatRepositoryImpl(connection)
            vilkårsResultatRepo.lagre(behandling.id, resultat)
        }

        val uthentetVilkårsResultat = dataSource.transaction { connection ->
            val behandlingId = behandling.id
            val vilkårsResultatRepo = VilkårsresultatRepositoryImpl(connection)
            vilkårsResultatRepo.hent(behandlingId)
        }

        assertThat(uthentetVilkårsResultat.alle().size).isEqualTo(3)
        assertThat(uthentetVilkårsResultat.alle()).containsExactlyInAnyOrder(*(resultat.alle()).toTypedArray())
    }
}