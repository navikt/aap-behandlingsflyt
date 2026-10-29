package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgRegisterGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgFaktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.MedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MedlemskapvilkåretTest {
    private val rettighetsperiode = Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))

    @Test
    fun `delvurdering som kan behandles automatisk gir oppfylt`() {
        val utfall = vurder(grunnlag(delvurdering = delvurdering(true)))

        assertEquals(Utfall.OPPFYLT, utfall)
    }

    @Test
    fun `delvurdering som ikke kan behandles automatisk gir ikke vurdert`() {
        val utfall = vurder(grunnlag(delvurdering = delvurdering(false)))

        assertEquals(Utfall.IKKE_VURDERT, utfall)
    }

    @Test
    fun `manglende delvurdering og manuell vurdering gir ikke vurdert`() {
        val utfall = vurder(grunnlag(delvurdering = null))

        assertEquals(Utfall.IKKE_VURDERT, utfall)
    }

    @Test
    fun `manglende søknad gir ikke relevant selv om delvurdering er oppfylt`() {
        val utfall = vurder(grunnlag(delvurdering = delvurdering(true), søknad = null))

        assertEquals(Utfall.IKKE_RELEVANT, utfall)
    }

    @Test
    fun `manuell vurdering prioriteres over delvurdering som er oppfylt`() {
        val utfall = vurder(
            grunnlag(
                delvurdering = delvurdering(true),
                manuelleVurderinger = listOf(manuellVurdering(medlem = false)),
            )
        )

        assertEquals(Utfall.IKKE_OPPFYLT, utfall)
    }

    @Test
    fun `manuell vurdering prioriteres over delvurdering som ikke er oppfylt`() {
        val utfall = vurder(
            grunnlag(
                delvurdering = delvurdering(false),
                manuelleVurderinger = listOf(manuellVurdering(medlem = true)),
            )
        )

        assertEquals(Utfall.OPPFYLT, utfall)
    }

    @Test
    fun `vurderingsgrunnlag serialiseres med register, delvurdering og manuelle vurderinger`() {
        val json = DefaultJsonMapper.objectMapper().readTree(
            grunnlag(
                delvurdering = delvurdering(true),
                manuelleVurderinger = listOf(manuellVurdering(medlem = true)),
            ).hent()
        )

        assertEquals(true, json["delvurdering"]["kanBehandlesAutomatisk"].asBoolean())
        assertEquals(1, json["manuelleVurderinger"].size())
        assertEquals(true, json.has("register"))
    }

    private fun vurder(grunnlag: MedlemskapLovvalgFaktagrunnlag): Utfall {
        val vilkårsresultat = Vilkårsresultat()
        Medlemskapvilkåret(vilkårsresultat, rettighetsperiode).vurder(grunnlag)

        return vilkårsresultat.finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder().single().utfall
    }

    private fun delvurdering(kanBehandlesAutomatisk: Boolean) = LovvalgDelvurderingerGrunnlag(
        kanBehandlesAutomatisk = kanBehandlesAutomatisk,
        tilhørighetVurdering = emptyList(),
        relevantDato = rettighetsperiode.fom,
        delvurderingId = 1L,
    )

    private fun manuellVurdering(medlem: Boolean) = ManuellVurderingForLovvalgMedlemskap(
        lovvalg = LovvalgDto("begrunnelse", EØSLandEllerLandMedAvtale.NOR),
        medlemskap = MedlemskapDto("begrunnelse", medlem),
        vurdertAv = Bruker("saksbehandler"),
        vurdertDato = LocalDateTime.of(2026, 1, 1, 12, 0),
        fom = rettighetsperiode.fom,
        vurdertIBehandling = BehandlingId(1L),
    )

    private fun grunnlag(
        delvurdering: LovvalgDelvurderingerGrunnlag?,
        manuelleVurderinger: List<ManuellVurderingForLovvalgMedlemskap> = emptyList(),
        søknad: UtenlandsOppholdData? = UtenlandsOppholdData(
            harBoddINorgeSiste5År = true,
            harArbeidetINorgeSiste5År = true,
            arbeidetUtenforNorgeFørSykdom = false,
            iTilleggArbeidUtenforNorge = false,
            utenlandsOpphold = null,
        ),
    ) = MedlemskapLovvalgFaktagrunnlag(
        register = MedlemskapLovvalgRegisterGrunnlag.fra(
            medlemskapArbeidInntektGrunnlag = null,
            personopplysning = null,
            nyeSoknadGrunnlag = søknad,
        ),
        delvurdering = delvurdering,
        manuelleVurderinger = manuelleVurderinger,
    )
}
