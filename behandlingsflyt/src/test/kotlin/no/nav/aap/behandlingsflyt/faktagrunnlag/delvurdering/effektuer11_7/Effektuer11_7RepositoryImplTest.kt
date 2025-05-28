package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.effektuer11_7

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.tomUnderveisInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7RepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.søknad.TrukketSøknadRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Effektuer11_7RepositoryImplTest {
    @Test
    fun `Finner ikke grunnlag hvis ikke lagret`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val effektuer11_7Grunnlag = Effektuer11_7RepositoryImpl(connection).hentHvisEksisterer(behandling.id)
            assertNull(effektuer11_7Grunnlag)
        }
    }

    @Test
    fun `Lagrer og henter varsel`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            val effektuer11_7Repository = Effektuer11_7RepositoryImpl(connection)
            val underveisRepository = UnderveisRepositoryImpl(connection)

            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val underveisperiode = underveisperiode(sak).copy(
                utfall = Utfall.IKKE_OPPFYLT,
                avslagsårsak = UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT,
            )
            underveisRepository.lagre(behandling.id, listOf(underveisperiode), tomUnderveisInput)

            val varsel = Effektuer11_7Forhåndsvarsel(
                referanse = null,
                datoVarslet = LocalDate.now(),
                frist = LocalDate.now().plusDays(1),
                underveisperioder = underveisRepository.hent(behandling.id).perioder,
            )

            effektuer11_7Repository.lagreVarsel(behandling.id, varsel)

            assertEquals(
                varsel,
                effektuer11_7Repository.hentHvisEksisterer(behandling.id)?.varslinger?.single()
            )
        }
    }

    @Test
    fun `Ny vurdering når grunnlag allerede finnes`(){
        InitTestDatabase.freshDatabase().transaction { connection ->
            val effektuer11_7Repository = Effektuer11_7RepositoryImpl(connection)
            val underveisRepository = UnderveisRepositoryImpl(connection)

            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val underveisperiode = underveisperiode(sak).copy(
                utfall = Utfall.IKKE_OPPFYLT,
                avslagsårsak = UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT,
            )
            underveisRepository.lagre(behandling.id, listOf(underveisperiode), tomUnderveisInput)

            val varsel = Effektuer11_7Forhåndsvarsel(
                referanse = null,
                datoVarslet = LocalDate.now(),
                frist = LocalDate.now().plusDays(1),
                underveisperioder = underveisRepository.hent(behandling.id).perioder,
            )

            effektuer11_7Repository.lagreVarsel(behandling.id, varsel)

            assertEquals(
                varsel,
                effektuer11_7Repository.hentHvisEksisterer(behandling.id)?.varslinger?.single()
            )

            val varsel2 = Effektuer11_7Forhåndsvarsel(
                referanse = null,
                datoVarslet = LocalDate.now().plusDays(1),
                frist = LocalDate.now().plusDays(2),
                underveisperioder = underveisRepository.hent(behandling.id).perioder,
            )

            effektuer11_7Repository.lagreVarsel(behandling.id, varsel2)
            assertEquals(
                varsel2,
                effektuer11_7Repository.hentHvisEksisterer(behandling.id)?.varslinger?.single()
            )
        }

    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection),
            BehandlingRepositoryImpl(connection),
            TrukketSøknadService(
                AvklaringsbehovRepositoryImpl(connection),
                TrukketSøknadRepositoryImpl(connection)
            ),
        ).finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
    }

    private fun underveisperiode(sak: Sak) = Underveisperiode(
        periode = sak.rettighetsperiode,
        meldePeriode = Periode(sak.rettighetsperiode.fom, sak.rettighetsperiode.fom.plusDays(14)),
        utfall = Utfall.OPPFYLT,
        rettighetsType = RettighetsType.BISTANDSBEHOV,
        avslagsårsak = null,
        grenseverdi = Prosent.`100_PROSENT`,
        arbeidsgradering = ArbeidsGradering(
            totaltAntallTimer = TimerArbeid(BigDecimal(0)),
            andelArbeid = Prosent.`0_PROSENT`,
            fastsattArbeidsevne = Prosent.`100_PROSENT`,
            gradering = Prosent.`100_PROSENT`,
            opplysningerMottatt = null,
        ),
        trekk = Dagsatser(0),
        brukerAvKvoter = setOf(),
        bruddAktivitetspliktId = null,
        institusjonsoppholdReduksjon = Prosent.`0_PROSENT`,
        meldepliktStatus = MeldepliktStatus.MELDT_SEG,
    )
}