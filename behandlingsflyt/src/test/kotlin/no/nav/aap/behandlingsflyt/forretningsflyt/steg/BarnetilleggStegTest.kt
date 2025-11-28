package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Relasjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class BarnetilleggStegTest {
    private lateinit var avklaringsbehovRepository: AvklaringsbehovRepository
    private lateinit var avklaringsbehovService: AvklaringsbehovService
    private lateinit var steg: BarnetilleggSteg
    private lateinit var barnRepository: BarnRepository
    private lateinit var tidligereVurderinger: TidligereVurderinger
    private lateinit var avbrytRevurderingService: AvbrytRevurderingService
    private val behandlingId = BehandlingId(Random().nextLong())

    @BeforeEach
    fun setup() {
        avklaringsbehovRepository = mockk()
        avbrytRevurderingService = mockk {
            every { revurderingErAvbrutt(any()) } returns false
        }

        barnRepository = mockk {
            every { hentHvisEksisterer(any()) } returns null
        }

        tidligereVurderinger = mockk {
            every { muligMedRettTilAAP(any(), StegType.BARNETILLEGG) } returns true
        }

        avklaringsbehovService = AvklaringsbehovService(avklaringsbehovRepository, avbrytRevurderingService)
        steg = BarnetilleggSteg(
            barnetilleggService = mockk(),
            barnetilleggRepository = mockk(),
            barnRepository = barnRepository,
            avklaringsbehovRepository = mockk(),
            tidligereVurderinger = tidligereVurderinger,
            avklaringsbehovService = mockk()
        )

    }

    @Test
    fun `vedtakBehøverVurdering returnerer false for meldekort`() {
        val kontekst = opprettKontekst(
            vurderingType = VurderingType.MELDEKORT,
            vurderingsbehov = listOf(Vurderingsbehov.BARNETILLEGG)
        )

        assertFalse(steg.vedtakBehøverVurdering(kontekst))
    }

    @Test
    fun `vedtakBehøverVurdering returnerer true ved førstegangsbehandling med dødsfall barn`() {
        val kontekst = opprettKontekst(
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            vurderingsbehov = listOf(Vurderingsbehov.DØDSFALL_BARN)
        )

        assertTrue(steg.vedtakBehøverVurdering(kontekst))
    }

    @Test
    fun `vedtakBehøverVurdering returnerer true ved revurdering med barnetillegg vurderingsbehov`() {
        val kontekst = opprettKontekst(
            vurderingType = VurderingType.REVURDERING,
            vurderingsbehov = listOf(Vurderingsbehov.BARNETILLEGG)
        )

        assertTrue(steg.vedtakBehøverVurdering(kontekst))
    }

    @Test
    fun `vedtakBehøverVurdering returnerer true når barn er oppgitt`() {
        val kontekst = opprettKontekst(
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            vurderingsbehov = listOf(Vurderingsbehov.HELHETLIG_VURDERING)
        )

        barnRepository = mockk {
            every { hentHvisEksisterer(any()) } returns BarnGrunnlag(
                null, OppgitteBarn(
                    1, listOf(
                        OppgitteBarn.OppgittBarn(
                            Ident("08427832180"), "Mille Larsen",
                            Fødselsdato(LocalDate.now().minusYears(3)), Relasjon.FORELDER
                        )
                    )
                ),
                null,
                null
            )
            every { lagreOppgitteBarn(any(), any()) }
        }

        tidligereVurderinger = mockk {
            every { muligMedRettTilAAP(any(), StegType.BARNETILLEGG) } returns true
        }

        steg = BarnetilleggSteg(
            barnetilleggService = mockk(),
            barnetilleggRepository = mockk(),
            barnRepository = barnRepository,
            avklaringsbehovRepository = mockk(),
            tidligereVurderinger = tidligereVurderinger,
            avklaringsbehovService = mockk()
        )

        assertTrue(steg.vedtakBehøverVurdering(kontekst))
    }

    @Test
    fun `vedtakBehøverVurdering returnerer false når ikke mulig med rett til AAP`() {
        val kontekst = opprettKontekst(
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            vurderingsbehov = listOf(Vurderingsbehov.BARNETILLEGG)
        )
        tidligereVurderinger = mockk {
            every { muligMedRettTilAAP(any(), StegType.BARNETILLEGG) } returns false
        }
        steg = BarnetilleggSteg(
            barnetilleggService = mockk(),
            barnetilleggRepository = mockk(),
            barnRepository = barnRepository,
            avklaringsbehovRepository = mockk(),
            tidligereVurderinger = tidligereVurderinger,
            avklaringsbehovService = mockk()
        )

        assertFalse(steg.vedtakBehøverVurdering(kontekst))
    }

    private fun opprettKontekst(
        vurderingType: VurderingType,
        vurderingsbehov: List<Vurderingsbehov>
    ): FlytKontekstMedPerioder {
        return FlytKontekstMedPerioder(
            sakId = mockk(),
            behandlingId = behandlingId,
            forrigeBehandlingId = null,
            behandlingType = mockk(),
            vurderingType = vurderingType,
            vurderingTypeRelevantForSteg = vurderingType,
            rettighetsperiode = Periode(LocalDate.now(), LocalDate.now().plusMonths(6)),
            vurderingsbehovRelevanteForSteg = vurderingsbehov.toSet()
        )
    }
}