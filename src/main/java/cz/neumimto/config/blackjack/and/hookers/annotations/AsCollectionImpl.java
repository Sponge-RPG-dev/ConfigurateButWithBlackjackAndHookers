package cz.neumimto.config.blackjack.and.hookers.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;

/**
 * Created by NeumimTo on 27.1.2019.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AsCollectionImpl {
    Class<? extends Collection> value();
}
