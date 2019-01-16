package cz.neumimto.config.blackjack.and.hookers.annotations;

import ninja.leaping.configurate.objectmapping.Setting;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Instructs the {@link  cz.neumimto.config.blackjack.and.hookers.NotSoStupidObjectMapper} that the static field may be (de)serialized.
 *
 * <p>Consumers should note the following:</p>
 * <ul>
 * <li>The annotated field must also be annotated with the {@link Setting} attribute in order for it to be considered by the object mapper;</li>
 * <li>Updatable set to true will allow the one specific instance of {@link  cz.neumimto.config.blackjack.and.hookers.NotSoStupidObjectMapper} </li>
 * </ul>
 *
 * <p>Note that these restrictions may be different when using derived object mappers. Consult with the documentation provided by those mappers to determine if this is the case.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Static {
    boolean updateable() default true;
}
