package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StoppetBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AlderFlytTest: AbstraktFlytOrkestratorTest(FakeUnleash::class) {
    @Test
    fun `Ikke oppfylt på grunn av alder på søknadstidspunkt`(hendelser: List<StoppetBehandling>) {
        var (_, behandling) = sendInnFørsteSøknad(person = TestPersoner.PERSON_FOR_UNG())

        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)

        val stegHistorikk = hentStegHistorikk(behandling.id)
        assertThat(stegHistorikk.map { it.steg() }).contains(StegType.BREV)
        assertThat(stegHistorikk.map { it.status() }).contains(StegStatus.AVKLARINGSPUNKT)

        //Henter vurder alder-vilkår
        //Assert utfall
        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
        val aldersvilkår = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)

        assertThat(aldersvilkår.vilkårsperioder())
            .hasSize(1)
            .noneMatch { vilkårsperiodeForAlder -> vilkårsperiodeForAlder.erOppfylt() }

        val status = behandling.status()
        assertThat(status).isEqualTo(Status.IVERKSETTES)

        val brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_AVSLAG)
        behandling =
            løsAvklaringsBehov(behandling, vedtaksbrevLøsning(brevbestilling.referanse.brevbestillingReferanse))

        val behov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(behov).isEmpty()

        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)
        assertThat(hendelser.last().behandlingStatus).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `stopper opp når søker er over 62 år`() {
        val fom = LocalDate.now()
        val periode = Periode(fom, fom.plusYears(3))
        val person = TestPersoner.PERSON_62()

        val (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
            periode = periode,
        )

        behandling
            .medKontekst {
                assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
                assertThat(åpneAvklaringsbehov).isNotEmpty()
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsFramTilGrunnlag(sak.rettighetsperiode.fom)
            .løsBeregningstidspunkt()
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).contains(Definisjon.VURDER_INNTEKTSBORTFALL)
            }
    }

    @Test
    fun `stopper ikke opp når søker er rett under 62 år gammel`() {
        val fom = LocalDate.now()
        val person = TestPersoner.PERSON_61()
        val periode = Periode(fom, fom.plusYears(3))

        val (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
            periode = periode
        )

        behandling
            .medKontekst {
                assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
                assertThat(åpneAvklaringsbehov).isNotEmpty()
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
        behandling.løsFramTilGrunnlag(sak.rettighetsperiode.fom)
            .løsBeregningstidspunkt()
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).noneMatch { it == Definisjon.VURDER_INNTEKTSBORTFALL }
            }
    }

}