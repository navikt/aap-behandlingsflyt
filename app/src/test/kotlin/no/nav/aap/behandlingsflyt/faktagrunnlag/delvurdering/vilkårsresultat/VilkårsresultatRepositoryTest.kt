package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårsresultatRepositoryTest {
    @Test
    fun `Test oprett vilkårsresultat og hent ut igjen`() {
        InitTestDatabase.dataSource.transaction { connection ->
            // SETUP
            // Opprett person, sak og behandling
            val personOgSakService = PersonOgSakService(connection, FakePdlGateway)
            val behandlingRepo = BehandlingRepositoryImpl(connection)
            val repo = VilkårsresultatRepository(connection)

            val sak =
                personOgSakService.finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
            val behandling = behandlingRepo.opprettBehandling(sak.id, listOf(), TypeBehandling.Førstegangsbehandling, null)
            val behandlingId = behandling.id

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
                                avslagsårsak = Avslagsårsak.BRUKER_OVER_67,
                                faktagrunnlag = null
                            )
                        )
                    )
                )
            )
            repo.lagre(behandlingId, resultat)

            val uthentetVilkårsResultat = repo.hent(behandlingId)

            assertThat(uthentetVilkårsResultat.alle().first().type).isEqualTo(resultat.alle().first().type)
            assertThat(uthentetVilkårsResultat.alle().first().vilkårsperioder()).isEqualTo(resultat.alle().first().vilkårsperioder())
            assertThat(uthentetVilkårsResultat.alle().size).isEqualTo(1)
            assertThat(uthentetVilkårsResultat.alle().first()).isEqualTo(resultat.alle().first())
        }
    }

}