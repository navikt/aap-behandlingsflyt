package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.FØRSTEGANGSBEHANDLING
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.MELDEKORT
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.REVURDERING
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class GrunnbeløpInformasjonskrav(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val unleashGateway: UnleashGateway,
) : Informasjonskrav<IngenInput, IngenRegisterData> {
    
    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.GRUNNBELØP

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): GrunnbeløpInformasjonskrav {
            return GrunnbeløpInformasjonskrav(
                tilkjentYtelseRepository = repositoryProvider.provide(),
                unleashGateway = gatewayProvider.provide(),
            )
        }
    }

    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return unleashGateway.isEnabled(BehandlingsflytFeature.GrunnbeloepInformasjonskrav)
                && kontekst.vurderingType in setOf(FØRSTEGANGSBEHANDLING, REVURDERING, MELDEKORT)
    }

    override fun klargjør(kontekst: FlytKontekstMedPerioder) = IngenInput

    override fun hentData(input: IngenInput) = IngenRegisterData

    override fun oppdater(
        input: IngenInput,
        registerdata: IngenRegisterData,
        kontekst: FlytKontekstMedPerioder
    ): Informasjonskrav.Endret {
        val tilkjentYtelsePerioder = tilkjentYtelseRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?: return IKKE_ENDRET

        // Fanger opp at G endres underveis i en periode med tilkjent ytelse:
        //
        //   Tilkjent:    |------------ G=124 028 ------------|
        //   Gjeldende G: |--- G=124 028 ---|--- G=130 160 ---|
        //                21. apr         1. mai            5. mai
        //                                  ↑
        //                            G endres her → ENDRET
        //
        val grunnbeløpTidslinje = Grunnbeløp.tilTidslinje()
        val harEndretGrunnbeløp = tilkjentYtelsePerioder.any { periode ->
            grunnbeløpTidslinje.begrensetTil(periode.periode).segmenter().any { segment ->
                segment.verdi != periode.tilkjent.grunnbeløp
            }
        }

        return if (harEndretGrunnbeløp) ENDRET else IKKE_ENDRET
    }

    override fun flettOpplysningerFraAtomærBehandling(kontekst: FlytKontekst): Informasjonskrav.Endret {
        // Trenger ikke å flette inn opplysninger da beregningen av tilkjent ytelse uansett kjøres på nytt
        return IKKE_ENDRET
    }
}
