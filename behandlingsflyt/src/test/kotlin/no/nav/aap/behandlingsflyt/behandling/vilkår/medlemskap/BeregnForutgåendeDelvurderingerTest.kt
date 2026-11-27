package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapFaktaGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.InntektINorgeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.FolkeregisterStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningMedHistorikk
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningMedHistorikkGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Statsborgerskap
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BeregnForutgåendeDelvurderingerTest {
    private val denneBehandlingen = BehandlingId(2)
    private val forrigeBehandling = BehandlingId(1)

    private val tidligDato = LocalDate.of(2024, 6, 1)
    private val senDato = LocalDate.of(2026, 6, 1)

    @Test
    fun `vurderer hver dato mot vinduet fra datoen og fem år tilbake`() {
        // Inntekt dekker nøyaktig fem år fram til tidligDato, men ikke vinduet fram til senDato
        val register = lagRegister(inntektsperiode = Periode(tidligDato.minusYears(5), tidligDato))

        val resultat = beregn(listOf(tidligDato, senDato), register = register)

        assertThat(resultat.map { it.relevantDato }).containsExactly(tidligDato, senDato)
        assertThat(resultat.single { it.relevantDato == tidligDato }.kanBehandlesAutomatisk).isTrue
        assertThat(resultat.single { it.relevantDato == senDato }.kanBehandlesAutomatisk).isFalse
    }

    @Test
    fun `inntekt etter relevant dato bidrar ikke til vurderingen`() {
        val register = lagRegister(inntektsperiode = Periode(tidligDato.plusDays(1), tidligDato.plusYears(5)))

        val resultat = beregn(listOf(tidligDato), register = register)

        assertThat(resultat.single().kanBehandlesAutomatisk).isFalse
    }

    @Test
    fun `gjenbruker forrige behandlings vurdering for datoer som ikke er nyeste`() {
        val register = lagRegister(inntektsperiode = Periode(tidligDato.minusYears(5), senDato))
        val fraForrige = delvurdering(tidligDato, kanBehandlesAutomatisk = false, vurdertIBehandling = forrigeBehandling)

        val resultat = beregn(
            listOf(tidligDato, senDato),
            forrige = listOf(fraForrige),
            register = register,
        )

        assertThat(resultat.single { it.relevantDato == tidligDato }).isSameAs(fraForrige)
        val nyesteDato = resultat.single { it.relevantDato == senDato }
        assertThat(nyesteDato.vurdertIBehandling).isEqualTo(denneBehandlingen)
        assertThat(nyesteDato.kanBehandlesAutomatisk).isTrue
    }

    @Test
    fun `beregner nyeste dato på nytt selv om den finnes i forrige behandling`() {
        val register = lagRegister(inntektsperiode = Periode(senDato.minusYears(5), senDato))
        val fraForrige = delvurdering(senDato, kanBehandlesAutomatisk = false, vurdertIBehandling = forrigeBehandling)

        val resultat = beregn(listOf(senDato), forrige = listOf(fraForrige), register = register).single()

        assertThat(resultat.kanBehandlesAutomatisk).isTrue
        assertThat(resultat.vurdertIBehandling).isEqualTo(denneBehandlingen)
    }

    @Test
    fun `beholder eksisterende vurdering når ny beregning gir samme resultat`() {
        val register = lagRegister(inntektsperiode = Periode(senDato.minusYears(5), senDato))
        val førsteKjøring = beregn(listOf(senDato), register = register, behandling = forrigeBehandling).single()

        val resultat = beregn(listOf(senDato), eksisterende = listOf(førsteKjøring), register = register).single()

        assertThat(resultat).isSameAs(førsteKjøring)
        assertThat(resultat.vurdertIBehandling).isEqualTo(forrigeBehandling)
    }

    @Test
    fun `erstatter eksisterende vurdering når registerdata gir nytt resultat`() {
        val register = lagRegister(inntektsperiode = Periode(senDato.minusYears(5), senDato))
        val eksisterende = delvurdering(senDato, kanBehandlesAutomatisk = false, vurdertIBehandling = forrigeBehandling)

        val resultat = beregn(listOf(senDato), eksisterende = listOf(eksisterende), register = register).single()

        assertThat(resultat.kanBehandlesAutomatisk).isTrue
        assertThat(resultat.vurdertIBehandling).isEqualTo(denneBehandlingen)
    }

    private fun beregn(
        datoer: List<LocalDate>,
        forrige: List<ForutgåendeDelvurderingerGrunnlag> = emptyList(),
        eksisterende: List<ForutgåendeDelvurderingerGrunnlag> = emptyList(),
        register: ForutgåendeMedlemskapFaktaGrunnlag,
        behandling: BehandlingId = denneBehandlingen,
    ) = BeregnForutgåendeDelvurderinger.kjørMedSporing(
        ForutgåendeDelvurderingerSporingGrunnlag(
            behandlingId = behandling,
            relevanteDatoer = datoer,
            forrigeBehandlingsVurderinger = forrige,
            eksisterendeVurderinger = eksisterende,
            register = register,
        )
    )

    private fun delvurdering(
        dato: LocalDate,
        kanBehandlesAutomatisk: Boolean,
        vurdertIBehandling: BehandlingId,
    ) = ForutgåendeDelvurderingerGrunnlag(
        kanBehandlesAutomatisk = kanBehandlesAutomatisk,
        tilhørighetVurdering = emptyList(),
        relevantDato = dato,
        vurdertIBehandling = vurdertIBehandling,
    )

    private fun lagRegister(inntektsperiode: Periode) = ForutgåendeMedlemskapFaktaGrunnlag(
        forutgåendeMedlemskapGrunnlag = ForutgåendeMedlemskapGrunnlag(
            medlemskapUnntakGrunnlag = null,
            inntekterINorgeGrunnlag = listOf(
                InntektINorgeGrunnlag("1", 1.0, "NOR", "NOR", "type", inntektsperiode, "orgname")
            ),
            arbeiderINorgeGrunnlag = emptyList(),
            vurderinger = emptyList(),
        ),
        personopplysningGrunnlag = PersonopplysningMedHistorikkGrunnlag(
            brukerPersonopplysning = PersonopplysningMedHistorikk(
                fødselsdato = Fødselsdato(LocalDate.of(1990, 1, 1)),
                id = 1,
                dødsdato = null,
                statsborgerskap = listOf(Statsborgerskap("NOR")),
                folkeregisterStatuser = listOf(
                    FolkeregisterStatus(
                        status = PersonStatus.bosatt,
                        gyldighetstidspunkt = LocalDate.of(1990, 1, 1),
                        opphoerstidspunkt = null
                    )
                )
            ),
        ),
        nyeSoknadGrunnlag = UtenlandsOppholdData(true, true, false, false, null)
    )
}
