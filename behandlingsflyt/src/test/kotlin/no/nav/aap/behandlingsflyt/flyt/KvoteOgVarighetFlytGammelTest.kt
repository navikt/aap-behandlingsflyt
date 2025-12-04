package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plussEtÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.G_REGULERING
import no.nav.aap.behandlingsflyt.test.ProdlikUnleash
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Test

/** Gammel adferd vi ønsker å fase ut. */
class KvoteOgVarighetFlytGammelTest: AbstraktFlytOrkestratorTest(ProdlikUnleash::class) {
    @Test
    fun `manglende meldekort tolkes som reduksjon`() {
        val kravdato = 20 januar 2025 /* mandag */
        val sak = happyCaseFørstegangsbehandling(fom = kravdato, periode = Periode(kravdato, kravdato.plusYears(4)))
        val førstegangsbehandling = hentSisteOpprettedeBehandlingForSak(sak.saksnummer)

        /** Meldeplikten anses som oppfylt i førstegangsvedtaket, så vi får rettighetstypen satt
         * kvoten er brukt opp.
         */
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

        /** Bruker har ikke sendt noen meldekort, så kun første meldeperiode etter at vedtaket ble
         * fattet har rettighetstype. */
        revurdering.assertRettighetstype(
            Periode(kravdato, 2 februar 2025) to RettighetsType.BISTANDSBEHOV,
        )
    }
}