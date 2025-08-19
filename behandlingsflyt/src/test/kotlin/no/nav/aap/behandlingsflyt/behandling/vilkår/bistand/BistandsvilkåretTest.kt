package no.nav.aap.behandlingsflyt.behandling.vilkår.bistand

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarBistandLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandVurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderBistandsbehovSteg
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.bistand.BistandRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class BistandsvilkåretTest {
    private val dataSource = InitTestDatabase.freshDatabase()

    private val gatewayProvider = createGatewayProvider {
        register<FakeUnleash>()
    }

    @Test
    fun `nye vurderinger skal overskrive`() {
        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.BISTANDSVILKÅRET)

        Bistandsvilkåret(vilkårsresultat).vurder(
            BistandFaktagrunnlag(
                vurderingsdato = LocalDate.now(),
                sisteDagMedMuligYtelse = LocalDate.now().plusYears(3),
                vurderinger = listOf(bistandvurdering()),
                studentvurdering = null
            )
        )
        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)

        assertThat(vilkår.vilkårsperioder()).hasSize(1).allMatch { periode -> periode.utfall == Utfall.OPPFYLT }

        Bistandsvilkåret(vilkårsresultat).vurder(
            BistandFaktagrunnlag(
                vurderingsdato = LocalDate.now(),
                sisteDagMedMuligYtelse = LocalDate.now().plusYears(3),
                vurderinger = listOf(
                    bistandvurdering(
                        erBehovForAktivBehandling = false,
                        erBehovForAnnenOppfølging = false,
                        erBehovForArbeidsrettetTiltak = false
                    )
                ),
                studentvurdering = null
            )
        )
        assertThat(vilkår.vilkårsperioder()).hasSize(1).allMatch { periode -> periode.utfall == Utfall.IKKE_OPPFYLT }
    }

    @Test
    fun `Skal kunne ha vurderinger med ulike utfall`() {
        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.BISTANDSVILKÅRET)

        val iDag = LocalDate.now()
        Bistandsvilkåret(vilkårsresultat).vurder(
            BistandFaktagrunnlag(
                vurderingsdato = iDag,
                sisteDagMedMuligYtelse = LocalDate.now().plusYears(3),
                vurderinger = listOf(
                    bistandvurdering(), bistandvurdering(
                        vurderingenGjelderFra = iDag.plusDays(10),
                        erBehovForAktivBehandling = false,
                        erBehovForAnnenOppfølging = false,
                        erBehovForArbeidsrettetTiltak = false
                    )
                ),
                studentvurdering = null
            )
        )

        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)

        assertThat(vilkår.vilkårsperioder()).hasSize(2)
        assertThat(vilkår.vilkårsperioder().first().utfall).isEqualTo(Utfall.OPPFYLT)
        assertThat(vilkår.vilkårsperioder().last().innvilgelsesårsak).isNull()
        assertThat(vilkår.vilkårsperioder().last().utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        assertThat(vilkår.vilkårsperioder().last().avslagsårsak).isEqualTo(Avslagsårsak.IKKE_BEHOV_FOR_OPPFOLGING)
        assertThat(vilkår.vilkårsperioder().last().periode.fom).isEqualTo(iDag.plusDays(10))
    }

    @Test
    fun `Skal kunne innvilge 11-18`() {
        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.BISTANDSVILKÅRET)

        val iDag = LocalDate.now()
        Bistandsvilkåret(vilkårsresultat).vurder(
            BistandFaktagrunnlag(
                vurderingsdato = iDag,
                sisteDagMedMuligYtelse = LocalDate.now().plusYears(3),
                vurderinger = listOf(
                    bistandvurdering(),
                    bistandvurdering(
                        vurderingenGjelderFra = iDag.plusDays(10),
                        erBehovForAktivBehandling = false,
                        erBehovForAnnenOppfølging = false,
                        erBehovForArbeidsrettetTiltak = false,
<<<<<<< HEAD
                        skalVurdereAapIOvergangTilArbeid = true,
=======
                        skalVurdereAapIOvergangTilUføre = false
>>>>>>> dad43f5f5 (Begynner på jobben om overgang Arbeidssøker)
                    )
                ),
                studentvurdering = null
            )
        )

        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)

        assertThat(vilkår.vilkårsperioder()).hasSize(2)
        assertThat(vilkår.vilkårsperioder()).allMatch { it.utfall == Utfall.OPPFYLT }
        assertThat(vilkår.vilkårsperioder().last().innvilgelsesårsak).isEqualTo(Innvilgelsesårsak.ARBEIDSSØKER)
    }

    @Test
    fun `Skal kunne innvilge 11-17`() {
        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.BISTANDSVILKÅRET)

        val iDag = LocalDate.now()
        Bistandsvilkåret(vilkårsresultat).vurder(
            BistandFaktagrunnlag(
                vurderingsdato = iDag,
                sisteDagMedMuligYtelse = LocalDate.now().plusYears(3),
                vurderinger = listOf(
                    bistandvurdering(),
                    bistandvurdering(
                        vurderingenGjelderFra = iDag.plusDays(10),
                        erBehovForAktivBehandling = false,
                        erBehovForAnnenOppfølging = false,
                        erBehovForArbeidsrettetTiltak = false,
<<<<<<< HEAD
                        skalVurdereAapIOvergangTilArbeid = true,
=======
                        skalVurdereAapIOvergangTilUføre = true
>>>>>>> dad43f5f5 (Begynner på jobben om overgang Arbeidssøker)
                    )
                ),
                studentvurdering = null
            )
        )

        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)

        assertThat(vilkår.vilkårsperioder()).hasSize(2)
        assertThat(vilkår.vilkårsperioder()).allMatch { it.utfall == Utfall.OPPFYLT }
        assertThat(
            vilkår.vilkårsperioder().last().innvilgelsesårsak
        ).isEqualTo(Innvilgelsesårsak.ARBEIDSSØKER)
        assertThat(vilkår.vilkårsperioder().last().periode.fom).isEqualTo(iDag.plusDays(10))
    }

    @Test
    fun `Skal bygge tidslinje på tvers av behandlinger`() {
        val bistandsvurdering1 = BistandVurdering(
            begrunnelse = "Begrunnelse",
            erBehovForAktivBehandling = true,
            erBehovForArbeidsrettetTiltak = true,
            erBehovForAnnenOppfølging = false,
            vurderingenGjelderFra = null,
            vurdertAv = "Z00000",
<<<<<<< HEAD
            overgangBegrunnelse = null,
            skalVurdereAapIOvergangTilArbeid = null,
=======
            skalVurdereAapIOvergangTilUføre = null,
>>>>>>> dad43f5f5 (Begynner på jobben om overgang Arbeidssøker)
        )

        val (førstegangsbehandling, sak) = dataSource.transaction { connection ->
            val repo = BistandRepositoryImpl(connection)
            val sak = sak(connection)
            val førstegangsbehandling = finnEllerOpprettBehandling(connection, sak)

            repo.lagre(førstegangsbehandling.id, listOf(bistandsvurdering1))
            Pair(førstegangsbehandling, sak)
        }

        dataSource.transaction { connection ->
            val vilkårsresultat = VilkårsresultatRepositoryImpl(connection).hent(førstegangsbehandling.id)
            val rettighetsperiode = sak.rettighetsperiode
            Vilkårtype
                .entries
                .filter { it.obligatorisk }
                .forEach { vilkårstype ->
                    vilkårsresultat
                        .leggTilHvisIkkeEksisterer(vilkårstype)
                        .leggTilIkkeVurdertPeriode(rettighetsperiode)
                }

            VilkårsresultatRepositoryImpl(connection).lagre(førstegangsbehandling.id, vilkårsresultat)

        }

        dataSource.transaction { connection ->
            VurderBistandsbehovSteg.konstruer(postgresRepositoryRegistry.provider(connection), gatewayProvider).utfør(
                FlytKontekstMedPerioder(
                    sakId = sak.id,
                    behandlingId = førstegangsbehandling.id,
                    forrigeBehandlingId = førstegangsbehandling.forrigeBehandlingId,
                    behandlingType = TypeBehandling.Førstegangsbehandling,
                    vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
                    vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTTATT_SØKNAD),
                    rettighetsperiode = sak.rettighetsperiode,
                )
            )

            val vilkåret = VilkårsresultatRepositoryImpl(connection).hent(førstegangsbehandling.id)
                .finnVilkår(Vilkårtype.BISTANDSVILKÅRET)
            assertThat(vilkåret.vilkårsperioder()).hasSize(1)
        }

        val revurdering = dataSource.transaction { connection ->
            val revurdering = revurdering(connection, førstegangsbehandling, sak)
            revurdering
        }

        // Send inn revurderingsløsning
        dataSource.transaction { connection ->
            // Må lagre ned sykdomsvurdering for behandlingen da vurderingenGjelderFra for 11-6 skal være lik den for 11-5 i samme behandling
            val sykdomsvurdering = sykdomsvurdering(
                vurderingenGjelderFra = now.plusDays(10),
            )
            postgresRepositoryRegistry.provider(connection).provide<SykdomRepository>()
                .lagre(revurdering.id, listOf(sykdomsvurdering))

            val bistandsvurdering2 = BistandVurderingLøsningDto(
                begrunnelse = "Begrunnelse",
                erBehovForAktivBehandling = false,
                erBehovForArbeidsrettetTiltak = false,
                erBehovForAnnenOppfølging = false,
<<<<<<< HEAD
                skalVurdereAapIOvergangTilArbeid = false,
                overgangBegrunnelse = null,
=======
                skalVurdereAapIOvergangTilUføre = false,
>>>>>>> dad43f5f5 (Begynner på jobben om overgang Arbeidssøker)
            )

            AvklarBistandLøser(postgresRepositoryRegistry.provider(connection)).løs(
                AvklaringsbehovKontekst(
                    bruker = Bruker(sak.person.aktivIdent().identifikator),
                    kontekst = FlytKontekst(
                        behandlingId = revurdering.id,
                        forrigeBehandlingId = revurdering.forrigeBehandlingId,
                        sakId = sak.id,
                        behandlingType = TypeBehandling.Revurdering
                    ),
                ), løsning = AvklarBistandsbehovLøsning(bistandsVurdering = bistandsvurdering2)
            )
        }


        dataSource.transaction { connection ->
            VurderBistandsbehovSteg.konstruer(postgresRepositoryRegistry.provider(connection), gatewayProvider).utfør(
                FlytKontekstMedPerioder(
                    sakId = sak.id,
                    behandlingId = revurdering.id,
                    forrigeBehandlingId = revurdering.forrigeBehandlingId,
                    behandlingType = TypeBehandling.Revurdering,
                    vurderingType = VurderingType.REVURDERING,
                    vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTTATT_LEGEERKLÆRING),
                    rettighetsperiode = sak.rettighetsperiode,
                )
            )
        }

        dataSource.transaction { connection ->
            val vilkåret =
                VilkårsresultatRepositoryImpl(connection).hent(revurdering.id).finnVilkår(Vilkårtype.BISTANDSVILKÅRET)
            assertThat(vilkåret.vilkårsperioder()).hasSize(2)

            val segment1 = vilkåret.vilkårsperioder().first()
            val segment2 = vilkåret.vilkårsperioder().last()
            assertThat(segment1.periode).isEqualTo(Periode(now, now.plusDays(9)))
            assertThat(segment1.utfall).isEqualTo(Utfall.OPPFYLT)
            assertThat(segment2.periode).isEqualTo(
                Periode(
                    now.plusDays(10),
                    sak.rettighetsperiode.tom
                )
            )
            assertThat(segment2.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        }
    }


    private fun bistandvurdering(
        begrunnelse: String = "",
        erBehovForAktivBehandling: Boolean = true,
        erBehovForArbeidsrettetTiltak: Boolean = true,
        erBehovForAnnenOppfølging: Boolean = true,
<<<<<<< HEAD
        skalVurdereAapIOvergangTilArbeid: Boolean? = null,
=======
        skalVurdereAapIOvergangTilUføre: Boolean? = null,
>>>>>>> dad43f5f5 (Begynner på jobben om overgang Arbeidssøker)
        vurdertAv: String = "Z00000",
        vurderingenGjelderFra: LocalDate = LocalDate.now()

    ) = BistandVurdering(
        begrunnelse = begrunnelse,
        erBehovForAktivBehandling = erBehovForAktivBehandling,
        erBehovForArbeidsrettetTiltak = erBehovForArbeidsrettetTiltak,
        erBehovForAnnenOppfølging = erBehovForAnnenOppfølging,
<<<<<<< HEAD
        skalVurdereAapIOvergangTilArbeid = skalVurdereAapIOvergangTilArbeid,
=======
        skalVurdereAapIOvergangTilUføre = skalVurdereAapIOvergangTilUføre,
>>>>>>> dad43f5f5 (Begynner på jobben om overgang Arbeidssøker)
        vurdertAv = vurdertAv,
        overgangBegrunnelse = null,
        vurderingenGjelderFra = vurderingenGjelderFra
    )

    companion object {
        private val now = LocalDate.now()
        private val periode = Periode(now, LocalDate.now().plusYears(3))
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }

    private fun revurdering(connection: DBConnection, behandling: Behandling, sak: Sak): Behandling {
        BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling.id, Status.AVSLUTTET)

        return finnEllerOpprettBehandling(connection, sak)
    }

    private fun sykdomsvurdering(
        harSkadeSykdomEllerLyte: Boolean = true,
        erSkadeSykdomEllerLyteVesentligdel: Boolean = true,
        erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean = true,
        erNedsettelseIArbeidsevneAvEnVissVarighet: Boolean? = true,
        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean = true,
        erArbeidsevnenNedsatt: Boolean = true,
        vurderingenGjelderFra: LocalDate? = null,
        opprettet: Instant = Instant.now(),
    ) = Sykdomsvurdering(
        begrunnelse = "",
        dokumenterBruktIVurdering = emptyList(),
        harSkadeSykdomEllerLyte = harSkadeSykdomEllerLyte,
        erSkadeSykdomEllerLyteVesentligdel = erSkadeSykdomEllerLyteVesentligdel,
        erNedsettelseIArbeidsevneMerEnnHalvparten = erNedsettelseIArbeidsevneMerEnnHalvparten,
        erNedsettelseIArbeidsevneAvEnVissVarighet = erNedsettelseIArbeidsevneAvEnVissVarighet,
        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense,
        erArbeidsevnenNedsatt = erArbeidsevnenNedsatt,
        yrkesskadeBegrunnelse = null,
        vurderingenGjelderFra = vurderingenGjelderFra,
        vurdertAv = Bruker("Z00000"),
        opprettet = opprettet,
    )
}