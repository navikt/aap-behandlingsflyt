package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgFaktaGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgMedlemskapVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.MedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.Unntak
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Statsborgerskap
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MedlemskapvilkåretTest {
    private val rettighetsperiode = Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))

    @Test
    fun `uten lagrede vurderinger og automatisk grunnlag gir oppfylt uten manuell vurdering`() {
        val vurdering = vurder(grunnlag(vurderinger = emptyList()))

        assertThat(vurdering.utfall).isEqualTo(Utfall.OPPFYLT)
        assertThat(vurdering.manuellVurdering).isFalse
    }

    @Test
    fun `lagret automatisk vurdering gir samme utfall som uten, og regnes ikke som manuell`() {
        val vurdering = vurder(grunnlag(vurderinger = listOf(automatiskVurdering())))

        assertThat(vurdering.utfall).isEqualTo(Utfall.OPPFYLT)
        assertThat(vurdering.manuellVurdering).isFalse
    }

    @Test
    fun `lagret automatisk vurdering overstyrer ikke live-vurdering som ikke lenger kan behandles automatisk`() {
        val vurdering = vurder(
            grunnlag(
                vurderinger = listOf(automatiskVurdering()),
                kanBehandlesAutomatisk = false,
            )
        )

        assertThat(vurdering.utfall).isEqualTo(Utfall.IKKE_VURDERT)
        assertThat(vurdering.manuellVurdering).isFalse
    }

    @Test
    fun `lagret automatisk vurdering uten søknadsgrunnlag gir ikke relevant`() {
        val vurdering = vurder(
            grunnlag(
                vurderinger = listOf(automatiskVurdering()),
                medSøknadGrunnlag = false,
            )
        )

        assertThat(vurdering.utfall).isEqualTo(Utfall.IKKE_RELEVANT)
        assertThat(vurdering.manuellVurdering).isFalse
    }

    @Test
    fun `manuell vurdering brukes alene når den finnes`() {
        val vurdering = vurder(
            grunnlag(
                vurderinger = listOf(manuellVurdering(medlem = false)),
                kanBehandlesAutomatisk = false,
            )
        )

        assertThat(vurdering.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        assertThat(vurdering.manuellVurdering).isTrue
    }

    @Test
    fun `manuell vurdering vinner over nyere automatisk vurdering for samme periode`() {
        val manuell = manuellVurdering(medlem = false, behandling = 1, tidspunkt = LocalDateTime.of(2026, 2, 1, 12, 0))
        val automatisk = automatiskVurdering(behandling = 2, tidspunkt = LocalDateTime.of(2026, 3, 1, 12, 0))

        val vurdering = vurder(grunnlag(vurderinger = listOf(manuell, automatisk)))

        assertThat(vurdering.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        assertThat(vurdering.manuellVurdering).isTrue
    }

    private fun vurder(grunnlag: MedlemskapLovvalgFaktaGrunnlag) =
        Vilkårsresultat().also { Medlemskapvilkåret(it, rettighetsperiode).vurder(grunnlag) }
            .finnVilkår(Vilkårtype.LOVVALG)
            .tidslinje()
            .segmenter()
            .single()
            .verdi

    private fun grunnlag(
        vurderinger: List<LovvalgMedlemskapVurdering>,
        kanBehandlesAutomatisk: Boolean = true,
        medSøknadGrunnlag: Boolean = true,
    ) = MedlemskapLovvalgFaktaGrunnlag(
        medlemskapArbeidInntektGrunnlag = MedlemskapArbeidInntektGrunnlag(
            medlemskapGrunnlag = if (kanBehandlesAutomatisk) medlemskapGrunnlagMedMedlem() else null,
            inntekterINorgeGrunnlag = emptyList(),
            arbeiderINorgeGrunnlag = emptyList(),
            vurderinger = vurderinger,
        ),
        personopplysning = Personopplysning(
            Fødselsdato(LocalDate.of(2000, 1, 1)),
            null,
            PersonStatus.bosatt,
            listOf(Statsborgerskap("NOR")),
        ),
        nyeSoknadGrunnlag = if (medSøknadGrunnlag) UtenlandsOppholdData(
            harBoddINorgeSiste5År = true,
            harArbeidetINorgeSiste5År = true,
            arbeidetUtenforNorgeFørSykdom = false,
            iTilleggArbeidUtenforNorge = false,
            utenlandsOpphold = null,
        ) else null,
    )

    private fun medlemskapGrunnlagMedMedlem() = MedlemskapUnntakGrunnlag(
        unntak = listOf(
            Segment(
                periode = Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)),
                verdi = Unntak(
                    "unntak",
                    "statusaarsak",
                    true,
                    "grunnlag",
                    "lovvalg",
                    false,
                    EØSLandEllerLandMedAvtale.NOR.toString(),
                    null
                )
            )
        )
    )

    private fun automatiskVurdering(
        behandling: Long = 1,
        tidspunkt: LocalDateTime = LocalDateTime.of(2026, 1, 15, 12, 0),
    ) = vurdering(SYSTEMBRUKER, medlem = true, behandling = behandling, tidspunkt = tidspunkt)

    private fun manuellVurdering(
        medlem: Boolean,
        behandling: Long = 1,
        tidspunkt: LocalDateTime = LocalDateTime.of(2026, 1, 15, 12, 0),
    ) = vurdering(Bruker("Z000000"), medlem = medlem, behandling = behandling, tidspunkt = tidspunkt)

    private fun vurdering(
        vurdertAv: Bruker,
        medlem: Boolean,
        behandling: Long,
        tidspunkt: LocalDateTime,
    ) = LovvalgMedlemskapVurdering(
        lovvalg = LovvalgDto("Begrunnelse", EØSLandEllerLandMedAvtale.NOR),
        medlemskap = MedlemskapDto("Begrunnelse", medlem),
        vurdertAv = vurdertAv,
        vurdertDato = tidspunkt,
        fom = rettighetsperiode.fom,
        tom = null,
        vurdertIBehandling = BehandlingId(behandling),
    )
}
