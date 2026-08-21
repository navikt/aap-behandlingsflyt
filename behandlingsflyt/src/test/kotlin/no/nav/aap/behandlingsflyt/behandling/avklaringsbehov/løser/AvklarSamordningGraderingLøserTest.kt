package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningGraderingLøsning
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.VurderingerForSamordning
import no.nav.aap.behandlingsflyt.help.avklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningVurderingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningYtelseRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class AvklarSamordningGraderingLøserTest {

    @Test
    fun `skal kunne bekrefte kortet uten å legge inn noen perioder selv om register har funn`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()

        InMemorySamordningYtelseRepository.lagre(
            behandling.id,
            setOf(
                SamordningYtelse(
                    ytelseType = Ytelse.SYKEPENGER,
                    ytelsePerioder = setOf(
                        SamordningYtelsePeriode(
                            periode = Periode(LocalDate.now().minusYears(1), LocalDate.now()),
                            gradering = Prosent(100),
                        )
                    ),
                    kilde = "register",
                )
            )
        )

        val løser = AvklarSamordningGraderingLøser(inMemoryRepositoryProvider)

        løser.løs(
            avklaringsbehovKontekst { this.behandling = behandling },
            løsning = AvklarSamordningGraderingLøsning(
                vurderingerForSamordning = VurderingerForSamordning(
                    vurderteSamordningerData = emptyList(),
                    begrunnelse = "Ingen samordning aktuelt",
                ),
            )
        )

        val lagredeVurderinger = InMemorySamordningVurderingRepository.hentHvisEksisterer(behandling.id)
        assertThat(lagredeVurderinger?.vurderinger).isEmpty()
    }

    @Test
    fun `skal ikke kunne bekrefte kortet uten begrunnelse`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()

        val løser = AvklarSamordningGraderingLøser(inMemoryRepositoryProvider)

        assertThrows<UgyldigForespørselException> {
            løser.løs(
                avklaringsbehovKontekst { this.behandling = behandling },
                løsning = AvklarSamordningGraderingLøsning(
                    vurderingerForSamordning = VurderingerForSamordning(
                        vurderteSamordningerData = emptyList(),
                        begrunnelse = "",
                    ),
                )
            )
        }
    }
}
