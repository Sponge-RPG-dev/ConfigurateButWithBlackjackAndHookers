package cz.neumimto.config.blackjack.and.hookers.annotations;

import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Instructs the {@link  cz.neumimto.config.blackjack.and.hookers.NotSoStupidObjectMapper} that the serialization and deserialization value associated with the annotated field must be handled by the supplied {@link TypeSerializer} class
 *
 * <p>Consumers should note the following:</p>
 * <ul>
 * <li>The annotated field must also be annotated with the {@link Setting} attribute in order for it to be considered by the object mapper;</li>
 * <li>The supplied {@link TypeSerializer} class <strong>must</strong> have a public no-args constructor - violating this restriction will cause an {@link ObjectMappingException} during object mapper creation;</li>
 * <li>The {@link TypeSerializer} must deserialize into a type that the field can implicity accept (for example, you cannot use a {@link String} {@link TypeSerializer} on a field that is of type {@code int}) - violating this restriction will case an {@link ObjectMappingException} during serialization or deserialization.
 * </ul>
 *
 * <p>Note that these restrictions may be different when using derived object mappers. Consult with the documentation provided by those mappers to determine if this is the case.</p>
*/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CustomAdapter {

    Class<? extends TypeSerializer<?>> value();
}
