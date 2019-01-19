package cz.neumimto.config.blackjack.and.hookers.annotations;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Discriminator {
    String key();
    String value();
}
