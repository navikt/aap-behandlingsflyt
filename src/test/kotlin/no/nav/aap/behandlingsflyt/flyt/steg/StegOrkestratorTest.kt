package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.domene.Periode
import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.domene.person.Ident
import no.nav.aap.behandlingsflyt.domene.person.Personlager
import no.nav.aap.behandlingsflyt.domene.sak.Sakslager
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderSykdomSteg
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.grunnlag.person.Fødselsdato
import no.nav.aap.behandlingsflyt.grunnlag.person.PersonRegisterMock
import no.nav.aap.behandlingsflyt.grunnlag.person.Personinfo
import no.nav.aap.behandlingsflyt.grunnlag.student.db.InMemoryStudentRepository
import no.nav.aap.behandlingsflyt.grunnlag.yrkesskade.YrkesskadeRegisterMock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class StegOrkestratorTest {

    @Test
    fun `ved avklaringsbehov skal vi gå gjennom statusene START-UTFØRER-AVKARLINGSPUNKT`() {

        val ident = Ident("99999999911")
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        PersonRegisterMock.konstruer(ident, Personinfo(Fødselsdato(LocalDate.now().minusYears(18))))
        YrkesskadeRegisterMock.konstruer(ident = ident, periode = periode)

        val sak = Sakslager.finnEllerOpprett(Personlager.finnEllerOpprett(ident), periode)
        val behandling = BehandlingTjeneste.opprettBehandling(sak.id, emptyList())
        assertThat(behandling.type).isEqualTo(Førstegangsbehandling)

        initierVilkårenePåBehandlingen(behandling)

        val kontekst = FlytKontekst(sak.id, behandling.id)

        val resultat = StegOrkestrator(VurderSykdomSteg(BehandlingTjeneste, InMemoryStudentRepository)).utfør(kontekst, behandling)
        assertThat(resultat).isNotNull

        assertThat(behandling.stegHistorikk()).hasSize(3)
        assertThat(behandling.stegHistorikk().get(0).tilstand.status()).isEqualTo(StegStatus.START)
        assertThat(behandling.stegHistorikk().get(1).tilstand.status()).isEqualTo(StegStatus.UTFØRER)
        assertThat(behandling.stegHistorikk().get(2).tilstand.status()).isEqualTo(StegStatus.AVKLARINGSPUNKT)
    }

    private fun initierVilkårenePåBehandlingen(behandling: Behandling) {
        val vilkårsresultat = behandling.vilkårsresultat()
        val rettighetsperiode = Sakslager.hent(behandling.sakId).rettighetsperiode
        Vilkårtype.entries.forEach { vilkårstype ->
            vilkårsresultat.leggTilHvisIkkeEksisterer(vilkårstype).leggTilIkkeVurdertPeriode(rettighetsperiode)
        }
    }

}