import java.io.IOException
import java.util.jar.Manifest

class BuildVersion {

    companion object {

        //Get the JAR version directly from the manifest
        fun getVersion(): String = try {
            val manifestUrl = BuildVersion::class.java.classLoader.getResource("META-INF/MANIFEST.MF")
            Manifest(manifestUrl!!.openStream()).mainAttributes.getValue("Implementation-Version")
        } catch (e: IOException) {
            "0.0.0"
        }
    }
}