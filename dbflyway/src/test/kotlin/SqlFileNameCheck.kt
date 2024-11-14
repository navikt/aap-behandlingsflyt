import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.util.regex.Pattern


class SqlFileNameCheck {
    @Test
    fun `ingen æøå i migreringsskript`() {
        val resourceFiles = File("src/main/resources/flyway/").listFiles()

        assertThat(resourceFiles).isNotEmpty
        assertThat(resourceFiles!!.map { it.name }).allSatisfy { assertThat(it).endsWith(".sql") }
        assertThat(resourceFiles.map { it.name })
            .allSatisfy { assertThat(it).matches(Pattern.compile("^V(?:\\d{1,4}\\.)+\\d+__[A-Za-z_-]+\\.sql$")) }

    }
}