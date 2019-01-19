package cz.neumimto.config.test;

import cz.neumimto.config.blackjack.and.hookers.annotations.Discriminator;
import org.junit.jupiter.api.Test;

public class Tests {

    @Test
    public void test() {

    }


    @Discriminator(key = "type")
    public interface A {

    }
}
