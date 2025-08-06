package no.nav.aap.behandlingsflyt.behandling.brev

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOperasjonerRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Endring
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Brevbestilling
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.brev.kontrakt.Rolle
import no.nav.aap.brev.kontrakt.SignaturGrunnlag
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import java.util.*
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status as AvklaringsbehovStatus

@ExtendWith(MockKExtension::class)
@MockKExtension.CheckUnnecessaryStub
class SignaturServiceTest {
    @Test
    fun `den som står i signatur for en gitt rolle er den som utførte siste avklaringsbehovet for rollen (status AVSLUTTET)`() {

        val avklaringsbehovRepository = mockk<AvklaringsbehovRepository>()
        val avklaringsbehovOperasjonerRepository = mockk<AvklaringsbehovOperasjonerRepository>()

        val signaturService = SignaturService(avklaringsbehovRepository)

        val behandlingId = BehandlingId(1)
        val brevbestilling = Brevbestilling(
            id = 0,
            behandlingId = behandlingId,
            typeBrev = TypeBrev.VEDTAK_INNVILGELSE,
            referanse = BrevbestillingReferanse(UUID.randomUUID()),
            status = Status.FORHÅNDSVISNING_KLAR,
            opprettet = LocalDateTime.now()
        )
        val veilederIdent = "v000000"
        val kvalitetssikrerIdent = "k000000"
        val saksbehandlerIdent = "s000000"
        val beslutterIdent = "b000000"

        // SAKSBEHANDLER_OPPFOLGING
        leggTilEndring(Definisjon.FRITAK_MELDEPLIKT, endretAv = veilederIdent, AvklaringsbehovStatus.AVSLUTTET)
        leggTilEndring(Definisjon.AVKLAR_SYKDOM, endretAv = SYSTEMBRUKER.ident, AvklaringsbehovStatus.OPPRETTET)
        leggTilEndring(Definisjon.AVKLAR_SYKDOM, endretAv = veilederIdent, AvklaringsbehovStatus.AVSLUTTET)
        leggTilEndring(Definisjon.AVKLAR_SYKDOM, endretAv = kvalitetssikrerIdent, AvklaringsbehovStatus.KVALITETSSIKRET)
        leggTilEndring(Definisjon.AVKLAR_SYKDOM, endretAv = beslutterIdent, AvklaringsbehovStatus.TOTRINNS_VURDERT)

        // SAKSBEHANDLER_NASJONAL
        leggTilEndring(Definisjon.AVKLAR_STUDENT, endretAv = saksbehandlerIdent, AvklaringsbehovStatus.AVSLUTTET)
        leggTilEndring(
            Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT, endretAv = saksbehandlerIdent, AvklaringsbehovStatus.AVSLUTTET
        )

        // KVALITETSSIKRER
        leggTilEndring(Definisjon.KVALITETSSIKRING, endretAv = kvalitetssikrerIdent, AvklaringsbehovStatus.AVSLUTTET)

        // BESLUTTER
        leggTilEndring(Definisjon.FATTE_VEDTAK, endretAv = beslutterIdent, AvklaringsbehovStatus.AVSLUTTET)


        every { avklaringsbehovOperasjonerRepository.hent(behandlingId) } returns avklaringsbehovene
        every { avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId) } returns Avklaringsbehovene(
            avklaringsbehovOperasjonerRepository,
            behandlingId
        )

        val signaturer = signaturService.finnSignaturGrunnlag(brevbestilling, Bruker(""))

        assertThat(signaturer).containsExactly(
            SignaturGrunnlag(navIdent = beslutterIdent, rolle = Rolle.BESLUTTER),
            SignaturGrunnlag(navIdent = saksbehandlerIdent, rolle = Rolle.SAKSBEHANDLER_NASJONAL),
            SignaturGrunnlag(navIdent = kvalitetssikrerIdent, rolle = Rolle.KVALITETSSIKRER),
            SignaturGrunnlag(navIdent = veilederIdent, rolle = Rolle.SAKSBEHANDLER_OPPFOLGING)
        )
    }

    @Test
    fun `skal fjerne duplikater om samme person har løst avklaringsbehov som er knytte til ulike roller – eksempel samme person på lokalkontor og NAY`() {

        val avklaringsbehovRepository = mockk<AvklaringsbehovRepository>()
        val avklaringsbehovOperasjonerRepository = mockk<AvklaringsbehovOperasjonerRepository>()

        val signaturService = SignaturService(avklaringsbehovRepository)

        val behandlingId = BehandlingId(1)
        val brevbestilling = Brevbestilling(
            id = 0,
            behandlingId = behandlingId,
            typeBrev = TypeBrev.VEDTAK_INNVILGELSE,
            referanse = BrevbestillingReferanse(UUID.randomUUID()),
            status = Status.FORHÅNDSVISNING_KLAR,
            opprettet = LocalDateTime.now()
        )
        val veilederIdent = "v000000"
        val kvalitetssikrerIdent = "k000000"
        val beslutterIdent = "b000000"

        // SAKSBEHANDLER_OPPFOLGING
        leggTilEndring(Definisjon.FRITAK_MELDEPLIKT, endretAv = veilederIdent, AvklaringsbehovStatus.AVSLUTTET)
        leggTilEndring(Definisjon.AVKLAR_SYKDOM, endretAv = SYSTEMBRUKER.ident, AvklaringsbehovStatus.OPPRETTET)
        leggTilEndring(Definisjon.AVKLAR_SYKDOM, endretAv = veilederIdent, AvklaringsbehovStatus.AVSLUTTET)
        leggTilEndring(Definisjon.AVKLAR_SYKDOM, endretAv = kvalitetssikrerIdent, AvklaringsbehovStatus.KVALITETSSIKRET)
        leggTilEndring(Definisjon.AVKLAR_SYKDOM, endretAv = beslutterIdent, AvklaringsbehovStatus.TOTRINNS_VURDERT)

        // SAKSBEHANDLER_NASJONAL
        leggTilEndring(Definisjon.AVKLAR_STUDENT, endretAv = veilederIdent, AvklaringsbehovStatus.AVSLUTTET)
        leggTilEndring(
            Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT, endretAv = veilederIdent, AvklaringsbehovStatus.AVSLUTTET
        )

        // KVALITETSSIKRER
        leggTilEndring(Definisjon.KVALITETSSIKRING, endretAv = kvalitetssikrerIdent, AvklaringsbehovStatus.AVSLUTTET)

        // BESLUTTER
        leggTilEndring(Definisjon.FATTE_VEDTAK, endretAv = beslutterIdent, AvklaringsbehovStatus.AVSLUTTET)


        every { avklaringsbehovOperasjonerRepository.hent(behandlingId) } returns avklaringsbehovene
        every { avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId) } returns Avklaringsbehovene(
            avklaringsbehovOperasjonerRepository,
            behandlingId
        )

        val signaturer = signaturService.finnSignaturGrunnlag(brevbestilling, Bruker(""))

        assertThat(signaturer).hasSize(3)
        assertThat(signaturer).containsExactlyInAnyOrder(
            SignaturGrunnlag(navIdent = beslutterIdent, rolle = Rolle.BESLUTTER),
            SignaturGrunnlag(navIdent = kvalitetssikrerIdent, rolle = Rolle.KVALITETSSIKRER),
            SignaturGrunnlag(navIdent = veilederIdent, rolle = Rolle.SAKSBEHANDLER_NASJONAL)
        )
    }

    private val avklaringsbehovene = mutableListOf<Avklaringsbehov>()

    private fun leggTilEndring(
        definisjon: Definisjon,
        endretAv: String,
        status: AvklaringsbehovStatus
    ) {
        val avklaringsbehov = avklaringsbehovene.find { it.definisjon == definisjon } ?: Avklaringsbehov(
            -1, definisjon, mutableListOf(), StegType.UDEFINERT, false
        ).also {
            avklaringsbehovene.add(it)
        }
        val tidsstempel = avklaringsbehov.historikk.maxOrNull()?.tidsstempel?.plusMinutes(1) ?: LocalDateTime.now()
        avklaringsbehov.historikk.add(
            Endring(
                status = status,
                tidsstempel = tidsstempel,
                begrunnelse = "",
                endretAv = endretAv
            )
        )
    }
}
