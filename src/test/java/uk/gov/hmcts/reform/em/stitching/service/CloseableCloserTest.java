package uk.gov.hmcts.reform.em.stitching.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CloseableCloserTest {

    @Mock
    private Closeable mockCloseable;

    @Test
    void testPrivateConstructorThrowsException() throws Exception {
        Constructor<CloseableCloser> constructor = CloseableCloser.class.getDeclaredConstructor();
        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        InvocationTargetException thrown = assertThrows(InvocationTargetException.class, constructor::newInstance);
        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("This is a utility class and cannot be instantiated", cause.getMessage());
    }

    @Test
    void closeWhenCloseableIsNull() throws IOException {
        CloseableCloser.close(null);
        verify(mockCloseable, never()).close();
    }

    @Test
    void close() throws IOException {
        CloseableCloser.close(mockCloseable);
        verify(mockCloseable, times(1)).close();
    }

    @Test
    void closeWhenCloseableCloseThrowsIOException() throws IOException {
        String exceptionMessage = "Simulated IOException during close";
        IOException ioException = new IOException(exceptionMessage);
        doThrow(ioException).when(mockCloseable).close();

        CloseableCloser.close(mockCloseable);

        verify(mockCloseable, times(1)).close();
    }

    @Test
    void closeWhenCloseableCloseThrowsRuntimeException() throws IOException {
        String exceptionMessage = "Simulated RuntimeException during close";
        RuntimeException runtimeException = new RuntimeException(exceptionMessage);
        doThrow(runtimeException).when(mockCloseable).close();

        CloseableCloser.close(mockCloseable);

        verify(mockCloseable, times(1)).close();
    }
}