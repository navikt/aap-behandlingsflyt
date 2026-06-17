package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import java.time.LocalDate
import java.util.UUID

class UføreInformasjonskravTest {

    private val sakService = mockk<SakService>()
    private val uføreRepository = mockk<UføreRepository>(relaxed = true)
    private val beregningVurderingRepository = mockk<BeregningVurderingRepository>(relaxed = true)
    private val uføreRegisterGateway = mockk<UføreRegisterGateway>(relaxed = true)
    private val tidligereVurderinger = mockk<TidligereVurderinger>(relaxed = true)

    private val informasjonskrav = UføreInformasjonskrav(
        sakService = sakService,
        uføreRepository = uføreRepository,
        beregningVurderingRepository = beregningVurderingRepository,
        uføreRegisterGateway = uføreRegisterGateway,
        tidligereVurderinger = tidligereVurderinger,
    )

    @Test
    fun `skal være relevant for overgang uføre stans`() {
        val behandlingId = BehandlingId(1)
        val kontekst = flytKontekstMedPerioder {
            this.behandlingId = behandlingId
            this.vurderingType = VurderingType.OVERGANG_UFORE_STANS
            this.rettighetsperiode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
        }
        every { sakService.hentSakFor(behandlingId) } returns sak()

        val resultat = informasjonskrav.erRelevant(kontekst, StegType.OVERGANG_UFORE, null)

        assertThat(resultat).isTrue()
    }

    private fun sak(): Sak {
        return Sak(
            id = SakId(1),
            saksnummer = Saksnummer("1"),
            person = Person(
                id = PersonId(1),
                referanse = UUID.randomUUID(),
                identer = listOf(Ident("12345678901"))
            ),
            rettighetsperiode = Periode(LocalDate.now(), LocalDate.now().plusYears(1)),
        )
    }
}
