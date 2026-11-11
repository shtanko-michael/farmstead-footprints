package online.farmsteadminecraft.footprints;

/**
 * Loader-specific services. Each loader entrypoint installs its implementation
 * into {@link #INSTANCE} before any common code runs.
 */
public interface ModLoaderAccess {
    Holder HOLDER = new Holder();

    static ModLoaderAccess get() {
        return HOLDER.access;
    }

    boolean isClient();

    boolean isServer();

    boolean isModLoaded(String id);

    String loaderName();

    /** The game's config folder ({@code .minecraft/config}). */
    java.nio.file.Path configDir();

    final class Holder {
        private ModLoaderAccess access;

        public void install(ModLoaderAccess access) {
            if (this.access != null) throw new IllegalStateException("ModLoaderAccess already installed");
            this.access = access;
        }
    }
}
