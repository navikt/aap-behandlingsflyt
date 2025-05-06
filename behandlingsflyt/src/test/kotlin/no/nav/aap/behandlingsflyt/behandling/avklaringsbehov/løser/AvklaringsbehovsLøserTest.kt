package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.integrasjon.unleash.UnleashService
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7RepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.SamordningAndreStatligeYtelserRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.SamordningUføreRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.SamordningYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.ytelsesvurdering.SamordningVurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektForutgåendeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.personopplysning.PersonopplysningForutgåendeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.personopplysning.PersonopplysningRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.barn.BarnRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.uføre.UføreRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.yrkesskade.YrkesskadeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.bistand.BistandRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.rettighetsperiode.VurderRettighetsperiodeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.student.StudentRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom.SykdomRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.søknad.TrukketSøknadRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.gateway.GatewayRegistry
import no.nav.aap.lookup.repository.RepositoryRegistry
import no.nav.aap.motor.FlytJobbRepositoryImpl
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AvklaringsbehovsLøserTest {

    @BeforeEach
    fun setUp() {
        RepositoryRegistry.register(PersonRepositoryImpl::class)
        RepositoryRegistry.register(SakRepositoryImpl::class)
        RepositoryRegistry.register(BehandlingRepositoryImpl::class)
        RepositoryRegistry.register(AvklaringsbehovRepositoryImpl::class)
        RepositoryRegistry.register(ArbeidsevneRepositoryImpl::class)
        RepositoryRegistry.register(Effektuer11_7RepositoryImpl::class)
        RepositoryRegistry.register(BistandRepositoryImpl::class)
        RepositoryRegistry.register(BeregningVurderingRepositoryImpl::class)
        RepositoryRegistry.register(SykdomRepositoryImpl::class)
        RepositoryRegistry.register(SykepengerErstatningRepositoryImpl::class)
        RepositoryRegistry.register<SamordningVurderingRepositoryImpl>()
        RepositoryRegistry.register<StudentRepositoryImpl>()
        RepositoryRegistry.register<MedlemskapArbeidInntektRepositoryImpl>()
        RepositoryRegistry.register<MeldepliktRepositoryImpl>()
        RepositoryRegistry.register<PersonopplysningForutgåendeRepositoryImpl>()
        RepositoryRegistry.register<MedlemskapArbeidInntektForutgåendeRepositoryImpl>()
        RepositoryRegistry.register<BarnRepositoryImpl>()
        RepositoryRegistry.register<InstitusjonsoppholdRepositoryImpl>()
        RepositoryRegistry.register<VilkårsresultatRepositoryImpl>()
        RepositoryRegistry.register<PersonopplysningRepositoryImpl>()
        RepositoryRegistry.register<BrevbestillingRepositoryImpl>()
        RepositoryRegistry.register<YrkesskadeRepositoryImpl>()
        RepositoryRegistry.register<SamordningUføreRepositoryImpl>()
        RepositoryRegistry.register<SamordningAndreStatligeYtelserRepositoryImpl>()
        RepositoryRegistry.register<RefusjonkravRepositoryImpl>()
        RepositoryRegistry.register<UføreRepositoryImpl>()
        RepositoryRegistry.register<SamordningYtelseRepositoryImpl>()
        RepositoryRegistry.register<TrukketSøknadRepositoryImpl>()
        RepositoryRegistry.register<MottattDokumentRepositoryImpl>()
        RepositoryRegistry.register<VurderRettighetsperiodeRepositoryImpl>()
        RepositoryRegistry.register<FlytJobbRepositoryImpl>()
        GatewayRegistry.register<FakeUnleash>()
    }

    @Test
    fun `alle subtyper skal ha unik verdi`() {
        val utledSubtypes = AvklaringsbehovsLøser::class.sealedSubclasses
        InitTestDatabase.freshDatabase().transaction { dbConnection ->
            val løsningSubtypes = utledSubtypes.map {
                it.constructors
                    .find { it.parameters.singleOrNull()?.type?.classifier == DBConnection::class }!!
                    .call(dbConnection).forBehov()
            }.toSet()

            Assertions.assertThat(løsningSubtypes).hasSize(utledSubtypes.size)
        }
    }
}