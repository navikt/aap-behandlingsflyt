package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plussEtÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.G_REGULERING
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KvoteOgVarighetFlytTest: AbstraktFlytOrkestratorTest(FakeUnleash::class) {
    @Test
    fun `manglende meldekort tolkes som reduksjon`() {
        val kravdato = 20 januar 2025 /* mandag */
        val sak = happyCaseFørstegangsbehandling(fom = kravdato, periode = Periode(kravdato, kravdato.plusYears(4)))
        val førstegangsbehandling = hentSisteOpprettedeBehandlingForSak(sak.saksnummer)

        førstegangsbehandling.assertUnderveis(
            Periode(kravdato, kravdato.plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR)) to {
                assertThat(it.brukerAvKvoter).isEqualTo(setOf(Kvote.ORDINÆR))
            }
        )
        førstegangsbehandling.assertRettighetstype(
            Periode(kravdato, kravdato.plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR)) to RettighetsType.BISTANDSBEHOV,
        )

        /** Juster vedtakstidspunkt så meldeplikten ikke automatisk blir oppfylt i neste behandling*/
        dataSource.transaction {
            it.execute("update vedtak set vedtakstidspunkt = ? where behandling_id = ?") {
                setParams {
                    setLocalDateTime(1, (30 januar 2025).atTime(14, 0, 0))
                    setLong(2, førstegangsbehandling.id.toLong())
                }
            }
        }

        /* Eneste som er viktig er at vi kjører underveis og tilkjent ytelse på nytt. */
        val revurdering = sak.opprettManuellRevurdering(G_REGULERING)

        /* Dagene teller fortsatt mot ordinær kvote. */
        førstegangsbehandling.assertUnderveis(
            Periode(kravdato, kravdato.plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR)) to {
                assertThat(it.brukerAvKvoter).isEqualTo(setOf(Kvote.ORDINÆR))
            }
        )

        /** Rettighetperioden er uendret siden vi reduserer. */
        revurdering.assertRettighetstype(
            Periode(kravdato, kravdato.plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR)) to RettighetsType.BISTANDSBEHOV,
        )
    }
}