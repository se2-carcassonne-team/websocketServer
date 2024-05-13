package at.aau.serg.websocketserver.helper;

import at.aau.serg.websocketserver.controller.helper.HelperMethods;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

public class HelperMethodsTests {
    @Test
    void testPrivateConstructor() {
        try {
            Constructor<HelperMethods> constructor = HelperMethods.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
            fail("Expected an IllegalStateException to be thrown");
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException e) {
            fail("Unexpected exception thrown: " + e.getMessage());
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof IllegalStateException, "Expected an IllegalStateException to be thrown, but got " + cause);
            assertEquals("Utility class", cause.getMessage());
        }
    }
}
