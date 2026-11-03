package com.sistemaventas.persistence;

import com.sistemaventas.inventory.adapter.InMemoryProductRegistrationRepository;
import com.sistemaventas.sales.adapter.InMemorySalesRepository;
import com.sistemaventas.store.StoreProfile;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Guarda productos, ventas y datos del comercio en un archivo local.
 * Escribe primero en un temporal y luego lo reemplaza, para no dejar el archivo a medias.
 */
public final class DataStore {
    private static final String FILE_NAME = "datos.bin";
    private final Path directory;

    public DataStore(Path directory) {
        this.directory = directory;
    }

    /** Carpeta de datos: propiedad {@code sistemaventas.datos}, variable SISTEMAVENTAS_DATOS o ~/.sistemaventas */
    public static DataStore defaultLocation() {
        String configured = System.getProperty("sistemaventas.datos", System.getenv("SISTEMAVENTAS_DATOS"));
        Path directory = configured == null || configured.isBlank()
                ? Path.of(System.getProperty("user.home"), ".sistemaventas")
                : Path.of(configured);
        return new DataStore(directory);
    }

    public record Snapshot(InMemoryProductRegistrationRepository.State inventory,
                           InMemorySalesRepository.State sales, StoreProfile store) implements Serializable {
    }

    public Path directory() {
        return directory;
    }

    public Path file() {
        return directory.resolve(FILE_NAME);
    }

    public Path photosDirectory() {
        return directory.resolve("fotos");
    }

    public Optional<Snapshot> load() throws IOException {
        if (!Files.exists(file())) {
            return Optional.empty();
        }
        try (InputStream input = Files.newInputStream(file()); ObjectInputStream objects = new ObjectInputStream(input)) {
            return Optional.of((Snapshot) objects.readObject());
        } catch (ClassNotFoundException | ClassCastException exception) {
            throw new IOException("El archivo de datos no es compatible: " + file(), exception);
        }
    }

    public synchronized void save(Snapshot snapshot) throws IOException {
        Files.createDirectories(directory);
        Path temporary = directory.resolve(FILE_NAME + ".tmp");
        try (OutputStream output = Files.newOutputStream(temporary); ObjectOutputStream objects = new ObjectOutputStream(output)) {
            objects.writeObject(snapshot);
        }
        try {
            Files.move(temporary, file(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, file(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
