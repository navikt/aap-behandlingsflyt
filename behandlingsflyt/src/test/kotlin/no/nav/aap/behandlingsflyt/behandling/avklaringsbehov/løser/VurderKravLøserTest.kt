package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import io.mockk.checkUnnecessaryStub
import io.mockk.clearMocks
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class VurderKravLøserTest {

    private val mottattDokumentRepository = mockk<MottattDokumentRepository>()
    private val kravRepository = mockk<KravRepository>()

    @AfterEach
    fun tearDown() {
        checkUnnecessaryStub(mottattDokumentRepository, kravRepository)
        clearMocks(mottattDokumentRepository, kravRepository)
    }

    @Test
    fun `skal feile hvis søknadsdato er ulik dato for mottatt søknad`() {
        TODO("Not yet implemented")
    }

    @Test
    fun `skal feile hvis mulig rett fra dato er etter søknadsdato`() {
        TODO("Not yet implemented")
    }

    @Test
    fun `skal feile for kravtyper som ikke er implementert`() {
        TODO("Not yet implemented")
    }

    @Test
    fun `skal mappe nytt krav`() {
        TODO("Not yet implemented")
    }


}