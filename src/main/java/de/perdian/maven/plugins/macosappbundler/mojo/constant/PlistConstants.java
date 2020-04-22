package de.perdian.maven.plugins.macosappbundler.mojo.constant;

final public class PlistConstants {

    private PlistConstants() {}

    public static final String JVM_MAIN_CLASS_NAME = "JVMMainClassName";
    public static final String JVM_MAIN_MODULE_NAME = "JVMMainModuleName";

    public static final String CF_BUNDLE_EXECUTABLE = "CFBundleExecutable";
    public static final String CF_BUNDLE_ICON_FILE = "CFBundleIconFile";

    public static final String JVM_RUNTIME_PATH = "JVMRuntimePath";
    public static final String NATIVE_LIBRARY_PATH = "NativeLibraryPath";
}
