package at.aau.serg.websocketserver.helper;

import at.aau.serg.websocketserver.controller.helper.HelperMethods;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class HelperMethodsTests {
    @Test
    void testPrivateConstructor() {
        try {
            Constructor<HelperMethods> constructor = HelperMethods.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            if (e instanceof InvocationTargetException) {
                Throwable cause = e.getCause();
                assertTrue(cause instanceof IllegalStateException, "Expected an IllegalStateException to be thrown, but got " + cause);
                assertEquals("Utility class", cause.getMessage());
            } else {
                fail("Unexpected exception thrown: " + e.getMessage());
            }
        }
    }
}
