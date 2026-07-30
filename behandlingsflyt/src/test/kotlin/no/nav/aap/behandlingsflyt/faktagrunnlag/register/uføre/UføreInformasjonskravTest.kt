package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBeregningVurderingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryPersonopplysningRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryUføreRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UføreInformasjonskravTest {

    private val sakService = SakService(InMemorySakRepository, InMemoryBehandlingRepository)
    private val uføreRepository = InMemoryUføreRepository
    private val beregningVurderingRepository = InMemoryBeregningVurderingRepository
    private val uføreRegisterGateway = object : UføreRegisterGateway {
        override fun innhentMedHistorikk(
            person: Person,
            fraDato: LocalDate
        ): Set<Uføre> = TODO()

        override fun hentÅpenUføreSøknad(person: Person): UføreSøknad = TODO()
    }
    private val tidligereVurderinger = FakeTidligereVurderinger()

    private val informasjonskrav = UføreInformasjonskrav(
        sakService = sakService,
        uføreRepository = uføreRepository,
        beregningVurderingRepository = beregningVurderingRepository,
        personopplysningRepository = InMemoryPersonopplysningRepository,
        uføreRegisterGateway = uføreRegisterGateway,
        tidligereVurderinger = tidligereVurderinger,
    )

    @Test
    fun `skal være relevant for overgang uføre stans`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val kontekst = flytKontekstMedPerioder {
            this.behandling = behandling
        }

        val resultat = informasjonskrav.erRelevant(kontekst, StegType.OVERGANG_UFORE, null)

        assertThat(resultat).isTrue()
    }
}
