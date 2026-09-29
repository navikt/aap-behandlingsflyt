package no.nav.aap.behandlingsflyt.arena

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ArenaMigreringMapperTest {

    private val fraArena = ArenaSykdomsvurderingResponse(
        vedtakId = 1,
        begrunnelse = "Bruker oppfyller vilkåret for 11-5",
        vilkar = listOf(
            ArenaVilkar(id = 1, kode = "INNTNEDS", status = "J", begrunnelse = null),
            ArenaVilkar(id = 2, kode = "SYKSKADLYT", status = "J", begrunnelse = null),
            ArenaVilkar(id = 3, kode = "AAARBEVNE", status = "J", begrunnelse = null),
        ),
        diagnoser = listOf(
            ArenaDiagnose(
                kodeverk = "ICD10",
                kode = "M797",
                type = ArenaDiagnoseType.HOVEDDIAGNOSE,
                opprettet = LocalDate.of(2016, 1, 1),
            ),
            ArenaDiagnose(
                kodeverk = "ICD10",
                kode = "M80",
                type = ArenaDiagnoseType.BIDIAGNOSE,
                opprettet = LocalDate.of(2016, 1, 1),
            )
        )
    )

    private val behandlingId = BehandlingId(1)
    private val fom: LocalDate = 1 januar 2024

    @Test
    fun `ArenaMigreringMapper mapper alle felter korrekt til Sykdomsvurdering fra Arena-respons`() {
        val vurdering = ArenaMigreringMapper.mapOppfyltOrdinærSykdomsvurdering(
            fraArena = fraArena,
            behandlingId = behandlingId,
            vurderingenGjelderFra = fom,
        )

        assertThat(vurdering.begrunnelse).isEqualTo("Automatisk migrert fra Arena\n\n${fraArena.begrunnelse}")
        assertThat(vurdering.vurderingenGjelderFra).isEqualTo(fom)
        assertThat(vurdering.vurderingenGjelderTil).isNull()
        assertThat(vurdering.diagnose?.kodeverk).isEqualTo("ICD10")
        assertThat(vurdering.diagnose?.hoveddiagnose).isEqualTo("M797")
        assertThat(vurdering.diagnose?.bidiagnoser).containsExactly("M80")
        assertThat(vurdering.harSkadeSykdomEllerLyte).isTrue()
        assertThat(vurdering.erSkadeSykdomEllerLyteVesentligdel).isTrue()
        assertThat(vurdering.erNedsettelseIArbeidsevneMerEnnHalvparten).isTrue()
        assertThat(vurdering.harNedsattArbeidsevne).isEqualTo(ArbeidsevneNedsattValg.JA)
        assertThat(vurdering.erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense).isNull()
        assertThat(vurdering.yrkesskadeBegrunnelse).isNull()
        assertThat(vurdering.vurdertAv).isEqualTo(SYSTEMBRUKER)
        assertThat(vurdering.vurdertIBehandling).isEqualTo(behandlingId)
        assertThat(vurdering.erOppfyltOrdinærMedUtlededeFelter()).isTrue()
    }

    @Test
    fun `mapSykdomsvurdering feiler med tydelig melding når Arena mangler hoveddiagnose`() {
        val utenHoveddiagnose = fraArena.copy(
            diagnoser = fraArena.diagnoser.filter { it.type == ArenaDiagnoseType.BIDIAGNOSE }
        )

        assertThatThrownBy {
            ArenaMigreringMapper.mapOppfyltOrdinærSykdomsvurdering(utenHoveddiagnose, behandlingId, fom)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("hoveddiagnose")
    }

    @Test
    fun `ArenaMigreringMapper mapper alle felter korrekt til Bistandsvurdering fra Arena-respons`() {
        val vurdering = ArenaMigreringMapper.mapOppfyltBistandsvurdering(
            behandlingId = behandlingId,
            fom = fom,
        )

        assertThat(vurdering.begrunnelse).isEqualTo("Ikke vurdert i Arena, men oppfylles automatisk ved at bruker har ordinær AAP i Arena på migreringstidspunktet")
        assertThat(vurdering.fom).isEqualTo(fom)
        assertThat(vurdering.tom).isNull()
        assertThat(vurdering.erBehovForAktivBehandling).isTrue()
        assertThat(vurdering.erBehovForArbeidsrettetTiltak).isTrue()
        assertThat(vurdering.erBehovForAnnenOppfølging).isNull()
        assertThat(vurdering.overgangBegrunnelse).isNull()
        assertThat(vurdering.skalVurdereAapIOvergangTilArbeid).isNull()
        assertThat(vurdering.vurdertAv).isEqualTo(SYSTEMBRUKER)
        assertThat(vurdering.vurdertIBehandling).isEqualTo(behandlingId)
    }
}
