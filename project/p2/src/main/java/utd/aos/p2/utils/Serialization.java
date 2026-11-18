package utd.aos.p2.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.Optional;

public class Serialization {
    public static <T> Optional<T> deserialize(String s) {
        try (var bais = new ByteArrayInputStream(Base64.getDecoder().decode(s));
                var ois = new ObjectInputStream(bais)) {

            @SuppressWarnings("unchecked")
            var result = (T) ois.readObject();

            return Optional.of(result);
        } catch (Exception _ex) {
            System.out.println("failed to deserialize object.\n");
            return Optional.empty();
        }
    }

    public static <T> String serialize(T obj) {
        try {
            var baos = new ByteArrayOutputStream();
            var oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception _ex) {
            System.out.println("failed to serialize object.");
            return "";
        }
    }
}
