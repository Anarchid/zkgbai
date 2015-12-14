package zkgbai;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.Platform;

import java.io.File;

/**
 * Created by haplo on 12/13/2015.
 */
public class LibLoader {
    private static final String LIBRARY_SYSTEM_PROPERTY = "java.library.path";
    private static final String LWJGL_LIBRARY_PROPERTY = "org.lwjgl.librarypath";
    private static final String JINPUT_LIBRARY_PROPERTY = "net.java.games.input.librarypath";

    public static final void load(){
        Platform OS = Platform.get();

        // points to the library directory with your jar libs and native lib folders, assumed to be in the same directory as your main jar.
        String path = new File(ZKGraphBasedAI.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getAbsolutePath() + File.separator + "jlib";
        String libpath; // used for pointing to the folder where the appropriate native libraries are.

        if (OS.equals(Platform.LINUX)) {
            libpath = path + File.separator + "linux";
        }else if (OS.equals(Platform.WINDOWS)) {
            libpath = path + File.separator + "windows";
        }else{ // If future developers want to support more operating systems then add them here. (I'll add MacOS support soon)
            throw new IllegalStateException("Encountered an unknown platform while loading native libraries");
        }

        //Add your library path and the path to the native libraries to the system library path.
        System.setProperty(LIBRARY_SYSTEM_PROPERTY, path + File.pathSeparator + libpath + File.pathSeparator + System.getProperty(LIBRARY_SYSTEM_PROPERTY));

        //Tell lwjgl where to look for native libraries
        System.setProperty(LWJGL_LIBRARY_PROPERTY, libpath);
    }
}
