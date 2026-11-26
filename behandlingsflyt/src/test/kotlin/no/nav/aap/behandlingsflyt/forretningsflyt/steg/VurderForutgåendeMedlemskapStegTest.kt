package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryForutgåendeMedlemskapRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryOppgittUtenlandsopplysningerRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVilkårsresultatRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.testGatewayProvider
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@Fakes
class VurderForutgåendeMedlemskapStegTest {

    private val steg = VurderForutgåendeMedlemskapSteg(inMemoryRepositoryProvider, testGatewayProvider())

    @Test
    fun `Uten registerdata og manuell vurdering åpnes avklaringsbehov og vilkåret er ikke vurdert`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 1 januar 2025)
        gjørPotensieltOppfyltRettighetstype(behandling, sak)
        oppgiUtenlandsopplysninger(behandling, harBoddINorgeSiste5År = false)

        assertThat(steg.utfør(førstegangsKontekst(sak, behandling))).isEqualTo(Fullført)

        assertThat(forutgåendeBehov(behandling)?.erÅpent()).isTrue
        assertThat(medlemskapUtfall(behandling)).containsOnly(Utfall.IKKE_VURDERT)
    }

    @Test
    fun `Steget er idempotent når det kjøres flere ganger uten ny informasjon`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 1 januar 2025)
        gjørPotensieltOppfyltRettighetstype(behandling, sak)
        oppgiUtenlandsopplysninger(behandling, harBoddINorgeSiste5År = false)
        val kontekst = førstegangsKontekst(sak, behandling)

        steg.utfør(kontekst)
        val behovEtterFørste = forutgåendeBehov(behandling)
        val vilkårEtterFørste = medlemskapUtfall(behandling)

        steg.utfør(kontekst)
        steg.utfør(kontekst)

        assertThat(forutgåendeBehov(behandling)?.historikk).isEqualTo(behovEtterFørste?.historikk)
        assertThat(forutgåendeBehov(behandling)?.status()).isEqualTo(behovEtterFørste?.status())
        assertThat(medlemskapUtfall(behandling)).isEqualTo(vilkårEtterFørste)
    }

    @Test
    fun `Oppfylt manuell vurdering gir oppfylt vilkår også ved gjentatte kjøringer`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 1 januar 2025)
        gjørPotensieltOppfyltRettighetstype(behandling, sak)
        oppgiUtenlandsopplysninger(behandling, harBoddINorgeSiste5År = false)
        val kontekst = førstegangsKontekst(sak, behandling)
        steg.utfør(kontekst)

        InMemoryForutgåendeMedlemskapRepository.lagreVurderinger(
            behandling.id,
            listOf(manuellVurdering(behandling, sak, harForutgåendeMedlemskap = true))
        )
        steg.utfør(kontekst)
        steg.utfør(kontekst)

        assertThat(medlemskapUtfall(behandling)).containsOnly(Utfall.OPPFYLT)
    }

    @Test
    fun `Ikke oppfylt manuell vurdering gir ikke oppfylt vilkår`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 1 januar 2025)
        gjørPotensieltOppfyltRettighetstype(behandling, sak)
        oppgiUtenlandsopplysninger(behandling, harBoddINorgeSiste5År = false)
        val kontekst = førstegangsKontekst(sak, behandling)
        steg.utfør(kontekst)

        InMemoryForutgåendeMedlemskapRepository.lagreVurderinger(
            behandling.id,
            listOf(manuellVurdering(behandling, sak, harForutgåendeMedlemskap = false))
        )
        steg.utfør(kontekst)

        assertThat(medlemskapUtfall(behandling)).containsOnly(Utfall.IKKE_OPPFYLT)
    }

    @Test
    fun `Vurderingstyper uten vilkårsvurdering lar vilkår og avklaringsbehov være urørt`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 1 januar 2025)
        gjørPotensieltOppfyltRettighetstype(behandling, sak)

        steg.utfør(kontekst(sak, behandling, VurderingType.MELDEKORT))

        assertThat(medlemskapUtfall(behandling)).isEmpty()
    }

    // Uten en potensielt oppfylt rettighetstype gir tidligere vurderinger uunngåelig avslag før medlemskapssteget
    private fun gjørPotensieltOppfyltRettighetstype(behandling: Behandling, sak: Sak) {
        val vilkårsresultat = InMemoryVilkårsresultatRepository.hent(behandling.id)
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.OVERGANGUFØREVILKÅRET).leggTilVurderinger(
            tidslinjeOf(
                sak.rettighetsperiode to Vilkårsvurdering(
                    utfall = Utfall.OPPFYLT,
                    manuellVurdering = true,
                    begrunnelse = null,
                    faktagrunnlag = null,
                )
            )
        )
        InMemoryVilkårsresultatRepository.lagre(behandling.id, vilkårsresultat)
    }

    private fun oppgiUtenlandsopplysninger(behandling: Behandling, harBoddINorgeSiste5År: Boolean) {
        InMemoryOppgittUtenlandsopplysningerRepository.lagreOppgittUtenlandsOppplysninger(
            behandling.id,
            JournalpostId("1"),
            UtenlandsOppholdData(
                harBoddINorgeSiste5År = harBoddINorgeSiste5År,
                harArbeidetINorgeSiste5År = true,
                arbeidetUtenforNorgeFørSykdom = false,
                iTilleggArbeidUtenforNorge = false,
                utenlandsOpphold = null
            )
        )
    }

    private fun førstegangsKontekst(sak: Sak, behandling: Behandling) =
        kontekst(sak, behandling, VurderingType.FØRSTEGANGSBEHANDLING)

    private fun kontekst(sak: Sak, behandling: Behandling, vurderingType: VurderingType): FlytKontekstMedPerioder =
        flytKontekstMedPerioder {
            this.behandling = behandling
            this.vurderingType = vurderingType
            this.vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTTATT_SØKNAD)
            this.rettighetsperiode = sak.rettighetsperiode
        }

    private fun forutgåendeBehov(behandling: Behandling): Avklaringsbehov? =
        InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
            .hentBehovForDefinisjon(Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP)

    private fun medlemskapUtfall(behandling: Behandling): List<Utfall> {
        val vilkårsresultat: Vilkårsresultat = InMemoryVilkårsresultatRepository.hent(behandling.id)
        return vilkårsresultat.optionalVilkår(Vilkårtype.MEDLEMSKAP)
            ?.vilkårsperioder()
            ?.map { it.utfall }
            .orEmpty()
    }

    private fun manuellVurdering(behandling: Behandling, sak: Sak, harForutgåendeMedlemskap: Boolean) =
        ManuellVurderingForForutgåendeMedlemskap(
            begrunnelse = "Begrunnelse",
            harForutgåendeMedlemskap = harForutgåendeMedlemskap,
            varMedlemMedNedsattArbeidsevne = null,
            medlemMedUnntakAvMaksFemAar = null,
            vurdertAv = Bruker("Saksbehandler"),
            vurdertTidspunkt = LocalDateTime.of(2025, 1, 2, 12, 0),
            vurdertIBehandling = behandling.id,
            fom = sak.rettighetsperiode.fom
        )
}
