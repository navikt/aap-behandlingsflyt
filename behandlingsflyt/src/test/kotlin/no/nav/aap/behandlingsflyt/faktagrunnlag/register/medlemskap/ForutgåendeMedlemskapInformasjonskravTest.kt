package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryForutgåendeMedlemskapRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.testGatewayProvider
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@Fakes
class ForutgåendeMedlemskapInformasjonskravTest {

    private val informasjonskrav = ForutgåendeMedlemskapInformasjonskrav.konstruer(
        inMemoryRepositoryProvider,
        testGatewayProvider()
    )

    @Test
    fun `Første innhenting av registerdata gir ENDRET`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 1 januar 2025)

        val resultat = oppdater(sak, behandling, forutgåendeRegisterdata())

        assertThat(resultat).isEqualTo(Informasjonskrav.Endret.ENDRET)
    }

    @Test
    fun `Identiske registerdata to ganger på rad gir IKKE_ENDRET`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 1 januar 2025)

        val førsteGang = oppdater(sak, behandling, forutgåendeRegisterdata())
        val andreGang = oppdater(sak, behandling, forutgåendeRegisterdata())

        assertThat(førsteGang).isEqualTo(Informasjonskrav.Endret.ENDRET)
        assertThat(andreGang).isEqualTo(Informasjonskrav.Endret.IKKE_ENDRET)
    }

    @Test
    fun `Endret inntekt gir ENDRET og oppdaterer grunnlaget`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 1 januar 2025)

        oppdater(sak, behandling, forutgåendeRegisterdata(beløp = 10_000.0))
        val resultat = oppdater(sak, behandling, forutgåendeRegisterdata(beløp = 20_000.0))

        assertThat(resultat).isEqualTo(Informasjonskrav.Endret.ENDRET)
        val inntekter = InMemoryForutgåendeMedlemskapRepository.hentHvisEksisterer(behandling.id)
            ?.inntekterINorgeGrunnlag.orEmpty()
        assertThat(inntekter.map { it.beloep }).containsExactly(20_000.0)
    }

    @Test
    fun `Manuelle vurderinger beholdes når registerdata endres`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 1 januar 2025)
        oppdater(sak, behandling, forutgåendeRegisterdata(beløp = 10_000.0))
        val vurdering = ManuellVurderingForForutgåendeMedlemskap(
            begrunnelse = "Begrunnelse",
            harForutgåendeMedlemskap = true,
            varMedlemMedNedsattArbeidsevne = null,
            medlemMedUnntakAvMaksFemAar = null,
            vurdertAv = Bruker("Saksbehandler"),
            vurdertTidspunkt = LocalDateTime.of(2025, 1, 2, 12, 0),
            vurdertIBehandling = behandling.id,
            fom = 1 januar 2025
        )
        InMemoryForutgåendeMedlemskapRepository.lagreVurderinger(behandling.id, listOf(vurdering))

        oppdater(sak, behandling, forutgåendeRegisterdata(beløp = 20_000.0))

        val grunnlag = InMemoryForutgåendeMedlemskapRepository.hentHvisEksisterer(behandling.id)
        assertThat(grunnlag?.vurderinger).containsExactly(vurdering)
    }

    private fun oppdater(
        sak: Sak,
        behandling: Behandling,
        registerdata: ForutgåendeMedlemskapInformasjonskrav.MedlemsskapReggisterdata
    ): Informasjonskrav.Endret {
        val kontekst = flytKontekstMedPerioder {
            this.behandling = behandling
            this.rettighetsperiode = sak.rettighetsperiode
        }
        return informasjonskrav.oppdater(
            ForutgåendeMedlemskapInformasjonskrav.MedlemsskapInput(sak),
            registerdata,
            kontekst
        )
    }
}
