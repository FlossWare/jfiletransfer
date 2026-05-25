package org.flossware.filetransfer;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for WebDavFileTransferClient to achieve 100% coverage.
 * Note: Most methods require Sardine connection which needs a live WebDAV server or complex mocking.
 * These tests focus on builder validation, configuration, and URL resolution logic.
 */
class WebDavFileTransferClientTest {

    private WebDavFileTransferClient client;

    @AfterEach
    void tearDown() throws Exception {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore close errors in tearDown
            }
        }
    }

    @Test
    @DisplayName("Should support builder chaining")
    void testBuilderChaining() {
        WebDavFileTransferClient.Builder builder = WebDavFileTransferClient.builder();
        assertSame(builder, builder.baseUrl("https://webdav.example.com/files/"));
        assertSame(builder, builder.username("user"));
        assertSame(builder, builder.password("pass"));
        assertSame(builder, builder.credentials("user", "pass"));
    }

    @Test
    @DisplayName("Should throw NullPointerException when baseUrl is null in builder")
    void testBuilderNullBaseUrl() {
        assertThrows(NullPointerException.class,
            () -> WebDavFileTransferClient.builder()
                .username("user")
                .password("pass")
                .build());
    }

    @Test
    @DisplayName("Should build client with authenticated access")
    void testBuildWithAuth() {
        client = WebDavFileTransferClient.builder()
            .baseUrl("https://webdav.example.com/files/")
            .username("user")
            .password("pass")
            .build();

        assertNotNull(client);
        String description = client.getDescription();
        assertTrue(description.contains("WebDAV["));
        assertTrue(description.contains("authenticated=true"));
    }

    @Test
    @DisplayName("Should build client with anonymous access")
    void testBuildWithoutAuth() {
        client = WebDavFileTransferClient.builder()
            .baseUrl("https://public.example.com/files/")
            .build();

        assertNotNull(client);
        String description = client.getDescription();
        assertTrue(description.contains("WebDAV["));
        assertTrue(description.contains("authenticated=false"));
    }

    @Test
    @DisplayName("Should add trailing slash to base URL if missing")
    void testBaseUrlTrailingSlash() {
        client = WebDavFileTransferClient.builder()
            .baseUrl("https://webdav.example.com/files")
            .build();

        String description = client.getDescription();
        assertTrue(description.contains("https://webdav.example.com/files/"));
    }

    @Test
    @DisplayName("Should not add extra trailing slash if already present")
    void testBaseUrlWithTrailingSlash() {
        client = WebDavFileTransferClient.builder()
            .baseUrl("https://webdav.example.com/files/")
            .build();

        String description = client.getDescription();
        assertFalse(description.contains("https://webdav.example.com/files//"));
    }

    @Test
    @DisplayName("Should resolve URL with path")
    void testResolveUrl() throws Exception {
        client = WebDavFileTransferClient.builder()
            .baseUrl("https://webdav.example.com/files/")
            .build();

        Method resolveUrl = WebDavFileTransferClient.class.getDeclaredMethod("resolveUrl", String.class);
        resolveUrl.setAccessible(true);

        String result = (String) resolveUrl.invoke(client, "document.pdf");
        assertEquals("https://webdav.example.com/files/document.pdf", result);
    }

    @Test
    @DisplayName("Should resolve URL with nested path")
    void testResolveUrlNestedPath() throws Exception {
        client = WebDavFileTransferClient.builder()
            .baseUrl("https://webdav.example.com/files/")
            .build();

        Method resolveUrl = WebDavFileTransferClient.class.getDeclaredMethod("resolveUrl", String.class);
        resolveUrl.setAccessible(true);

        String result = (String) resolveUrl.invoke(client, "uploads/document.pdf");
        assertEquals("https://webdav.example.com/files/uploads/document.pdf", result);
    }

    @Test
    @DisplayName("Should return description with authenticated access")
    void testGetDescriptionAuthenticated() {
        client = WebDavFileTransferClient.builder()
            .baseUrl("https://webdav.example.com/files/")
            .username("user")
            .password("pass")
            .build();

        String description = client.getDescription();
        assertTrue(description.contains("WebDAV["));
        assertTrue(description.contains("https://webdav.example.com/files/"));
        assertTrue(description.contains("authenticated=true"));
    }

    @Test
    @DisplayName("Should return description with anonymous access")
    void testGetDescriptionAnonymous() {
        client = WebDavFileTransferClient.builder()
            .baseUrl("https://public.example.com/files/")
            .build();

        String description = client.getDescription();
        assertTrue(description.contains("WebDAV["));
        assertTrue(description.contains("https://public.example.com/files/"));
        assertTrue(description.contains("authenticated=false"));
    }

    @Test
    @DisplayName("Should throw NullPointerException when constructor receives null baseUrl")
    void testConstructorNullBaseUrl() throws Exception {
        java.lang.reflect.Constructor<WebDavFileTransferClient> constructor =
            WebDavFileTransferClient.class.getDeclaredConstructor(
                String.class, String.class, String.class);
        constructor.setAccessible(true);

        java.lang.reflect.InvocationTargetException exception = assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> constructor.newInstance(null, null, null));

        assertTrue(exception.getCause() instanceof NullPointerException);
        assertTrue(exception.getCause().getMessage().contains("baseUrl cannot be null"));
    }

    @Test
    @DisplayName("Should handle null username and password in constructor")
    void testConstructorNullCredentials() throws Exception {
        client = createTestClient("https://webdav.example.com/files/", null, null);
        assertNotNull(client);
        String description = client.getDescription();
        assertTrue(description.contains("authenticated=false"));
    }

    @Test
    @DisplayName("Should use credentials builder method")
    void testCredentialsBuilder() {
        client = WebDavFileTransferClient.builder()
            .baseUrl("https://webdav.example.com/files/")
            .credentials("user", "pass")
            .build();

        String description = client.getDescription();
        assertTrue(description.contains("authenticated=true"));
    }

    @Test
    @DisplayName("Should verify DEFAULT_BUFFER_SIZE constant")
    void testConstantBufferSize() throws Exception {
        java.lang.reflect.Field bufferSize = WebDavFileTransferClient.class.getDeclaredField("DEFAULT_BUFFER_SIZE");
        bufferSize.setAccessible(true);
        assertEquals(8192, bufferSize.get(null));
    }

    @Test
    @DisplayName("Should handle HTTP base URL")
    void testHttpBaseUrl() {
        client = WebDavFileTransferClient.builder()
            .baseUrl("http://webdav.example.com/files/")
            .build();

        assertNotNull(client);
        String description = client.getDescription();
        assertTrue(description.contains("http://webdav.example.com/files/"));
    }

    @Test
    @DisplayName("Should handle HTTPS base URL")
    void testHttpsBaseUrl() {
        client = WebDavFileTransferClient.builder()
            .baseUrl("https://webdav.example.com/files/")
            .build();

        assertNotNull(client);
        String description = client.getDescription();
        assertTrue(description.contains("https://webdav.example.com/files/"));
    }

    @Test
    @DisplayName("Should handle base URL with custom port")
    void testBaseUrlWithCustomPort() {
        client = WebDavFileTransferClient.builder()
            .baseUrl("https://webdav.example.com:8443/files/")
            .build();

        assertNotNull(client);
        String description = client.getDescription();
        assertTrue(description.contains(":8443"));
    }

    @Test
    @DisplayName("Should handle base URL with path segments")
    void testBaseUrlWithPathSegments() {
        client = WebDavFileTransferClient.builder()
            .baseUrl("https://webdav.example.com/app/files/")
            .build();

        assertNotNull(client);
        String description = client.getDescription();
        assertTrue(description.contains("/app/files/"));
    }

    @Test
    @DisplayName("Should handle username without password")
    void testUsernameWithoutPassword() {
        client = WebDavFileTransferClient.builder()
            .baseUrl("https://webdav.example.com/files/")
            .username("user")
            .build();

        assertNotNull(client);
        String description = client.getDescription();
        // Authenticated is true if username is not null, even without password
        assertTrue(description.contains("authenticated=true"));
    }

    @Test
    @DisplayName("Should handle password without username")
    void testPasswordWithoutUsername() {
        client = WebDavFileTransferClient.builder()
            .baseUrl("https://webdav.example.com/files/")
            .password("pass")
            .build();

        assertNotNull(client);
        String description = client.getDescription();
        assertTrue(description.contains("authenticated=false"));
    }

    @Test
    @DisplayName("Should handle both username and password present")
    void testBothCredentialsPresent() {
        client = WebDavFileTransferClient.builder()
            .baseUrl("https://webdav.example.com/files/")
            .username("user")
            .password("pass")
            .build();

        assertNotNull(client);
        String description = client.getDescription();
        assertTrue(description.contains("authenticated=true"));
    }

    @Test
    @DisplayName("Should handle empty string credentials")
    void testEmptyStringCredentials() {
        client = WebDavFileTransferClient.builder()
            .baseUrl("https://webdav.example.com/files/")
            .username("")
            .password("")
            .build();

        assertNotNull(client);
        String description = client.getDescription();
        // Empty strings are not null, so authenticated should be true
        assertTrue(description.contains("authenticated=true"));
    }

    // Tests for actual WebDAV operations using mocked Sardine

    @Test
    @DisplayName("Should read file successfully")
    void testReadFileSuccess() throws Exception {
        byte[] fileContent = "test file content".getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent);

        try (MockedStatic<SardineFactory> factoryMock = mockStatic(SardineFactory.class)) {
            Sardine sardine = mock(Sardine.class);
            factoryMock.when(() -> SardineFactory.begin(anyString(), anyString())).thenReturn(sardine);
            when(sardine.get(anyString())).thenReturn(inputStream);

            client = WebDavFileTransferClient.builder()
                .baseUrl("https://webdav.example.com/files/")
                .username("user")
                .password("pass")
                .build();

            byte[] result = client.readFile("test.txt");
            assertArrayEquals(fileContent, result);
            verify(sardine).get("https://webdav.example.com/files/test.txt");
        }
    }

    @Test
    @DisplayName("Should throw IOException when readFile fails")
    void testReadFileFailure() throws Exception {
        try (MockedStatic<SardineFactory> factoryMock = mockStatic(SardineFactory.class)) {
            Sardine sardine = mock(Sardine.class);
            factoryMock.when(() -> SardineFactory.begin(anyString(), anyString())).thenReturn(sardine);
            when(sardine.get(anyString())).thenThrow(new IOException("File not found"));

            client = WebDavFileTransferClient.builder()
                .baseUrl("https://webdav.example.com/")
                .username("user")
                .password("pass")
                .build();

            IOException thrown = assertThrows(IOException.class, () -> client.readFile("missing.txt"));
            assertTrue(thrown.getMessage().contains("File not found"));
        }
    }

    @Test
    @DisplayName("Should open file successfully")
    void testOpenFileSuccess() throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream("test".getBytes());

        try (MockedStatic<SardineFactory> factoryMock = mockStatic(SardineFactory.class)) {
            Sardine sardine = mock(Sardine.class);
            factoryMock.when(() -> SardineFactory.begin(anyString(), anyString())).thenReturn(sardine);
            when(sardine.get(anyString())).thenReturn(inputStream);

            client = WebDavFileTransferClient.builder()
                .baseUrl("https://webdav.example.com/files/")
                .username("user")
                .password("pass")
                .build();

            InputStream result = client.openFile("test.txt");
            assertSame(inputStream, result);
        }
    }

    @Test
    @DisplayName("Should throw IOException when openFile fails")
    void testOpenFileFailure() throws Exception {
        try (MockedStatic<SardineFactory> factoryMock = mockStatic(SardineFactory.class)) {
            Sardine sardine = mock(Sardine.class);
            factoryMock.when(() -> SardineFactory.begin()).thenReturn(sardine);
            when(sardine.get(anyString())).thenThrow(new IOException("Cannot open file"));

            client = WebDavFileTransferClient.builder()
                .baseUrl("https://webdav.example.com/")
                .build();

            IOException thrown = assertThrows(IOException.class, () -> client.openFile("test.txt"));
            assertTrue(thrown.getMessage().contains("Cannot open file"));
        }
    }

    @Test
    @DisplayName("Should return true when file exists")
    void testExistsTrue() throws Exception {
        try (MockedStatic<SardineFactory> factoryMock = mockStatic(SardineFactory.class)) {
            Sardine sardine = mock(Sardine.class);
            factoryMock.when(() -> SardineFactory.begin()).thenReturn(sardine);
            when(sardine.exists(anyString())).thenReturn(true);

            client = WebDavFileTransferClient.builder()
                .baseUrl("https://webdav.example.com/")
                .build();

            assertTrue(client.exists("test.txt"));
            verify(sardine).exists("https://webdav.example.com/test.txt");
        }
    }

    @Test
    @DisplayName("Should return false when file does not exist")
    void testExistsFalse() throws Exception {
        try (MockedStatic<SardineFactory> factoryMock = mockStatic(SardineFactory.class)) {
            Sardine sardine = mock(Sardine.class);
            factoryMock.when(() -> SardineFactory.begin(anyString(), anyString())).thenReturn(sardine);
            when(sardine.exists(anyString())).thenReturn(false);

            client = WebDavFileTransferClient.builder()
                .baseUrl("https://webdav.example.com/files/")
                .username("user")
                .password("pass")
                .build();

            assertFalse(client.exists("missing.txt"));
        }
    }

    @Test
    @DisplayName("Should list files successfully")
    void testListSuccess() throws Exception {
        try (MockedStatic<SardineFactory> factoryMock = mockStatic(SardineFactory.class)) {
            Sardine sardine = mock(Sardine.class);
            factoryMock.when(() -> SardineFactory.begin()).thenReturn(sardine);

            DavResource dir = mock(DavResource.class);
            DavResource file1 = mock(DavResource.class);
            DavResource file2 = mock(DavResource.class);

            when(dir.getPath()).thenReturn("uploads/");
            when(file1.getPath()).thenReturn("/uploads/file1.txt");
            when(file2.getPath()).thenReturn("/uploads/file2.txt");

            List<DavResource> resources = new ArrayList<>();
            resources.add(dir);
            resources.add(file1);
            resources.add(file2);

            when(sardine.list(anyString())).thenReturn(resources);

            client = WebDavFileTransferClient.builder()
                .baseUrl("https://webdav.example.com/")
                .build();

            List<String> result = client.list("uploads/");
            assertEquals(2, result.size());
            assertTrue(result.contains("uploads/file1.txt"));
            assertTrue(result.contains("uploads/file2.txt"));
        }
    }

    @Test
    @DisplayName("Should filter directory itself from list results")
    void testListFilterDirectory() throws Exception {
        try (MockedStatic<SardineFactory> factoryMock = mockStatic(SardineFactory.class)) {
            Sardine sardine = mock(Sardine.class);
            factoryMock.when(() -> SardineFactory.begin(anyString(), anyString())).thenReturn(sardine);

            DavResource dir = mock(DavResource.class);
            when(dir.getPath()).thenReturn("docs");

            List<DavResource> resources = Collections.singletonList(dir);
            when(sardine.list(anyString())).thenReturn(resources);

            client = WebDavFileTransferClient.builder()
                .baseUrl("https://webdav.example.com/")
                .username("user")
                .password("pass")
                .build();

            List<String> result = client.list("docs");
            assertTrue(result.isEmpty());
        }
    }

    @Test
    @DisplayName("Should handle paths without leading slash")
    void testListPathNormalization() throws Exception {
        try (MockedStatic<SardineFactory> factoryMock = mockStatic(SardineFactory.class)) {
            Sardine sardine = mock(Sardine.class);
            factoryMock.when(() -> SardineFactory.begin()).thenReturn(sardine);

            DavResource file = mock(DavResource.class);
            when(file.getPath()).thenReturn("uploads/file.txt");

            List<DavResource> resources = Collections.singletonList(file);
            when(sardine.list(anyString())).thenReturn(resources);

            client = WebDavFileTransferClient.builder()
                .baseUrl("https://webdav.example.com/")
                .build();

            List<String> result = client.list("uploads");
            assertEquals(1, result.size());
            assertEquals("uploads/file.txt", result.get(0));
        }
    }

    @Test
    @DisplayName("Should throw IOException when list fails")
    void testListFailure() throws Exception {
        try (MockedStatic<SardineFactory> factoryMock = mockStatic(SardineFactory.class)) {
            Sardine sardine = mock(Sardine.class);
            factoryMock.when(() -> SardineFactory.begin()).thenReturn(sardine);
            when(sardine.list(anyString())).thenThrow(new IOException("Directory not found"));

            client = WebDavFileTransferClient.builder()
                .baseUrl("https://webdav.example.com/")
                .build();

            IOException thrown = assertThrows(IOException.class, () -> client.list("missing"));
            assertTrue(thrown.getMessage().contains("Directory not found"));
        }
    }

    @Test
    @DisplayName("Should get file size successfully")
    void testGetFileSizeSuccess() throws Exception {
        try (MockedStatic<SardineFactory> factoryMock = mockStatic(SardineFactory.class)) {
            Sardine sardine = mock(Sardine.class);
            factoryMock.when(() -> SardineFactory.begin(anyString(), anyString())).thenReturn(sardine);

            DavResource resource = mock(DavResource.class);
            when(resource.isDirectory()).thenReturn(false);
            when(resource.getContentLength()).thenReturn(12345L);

            List<DavResource> resources = Collections.singletonList(resource);
            when(sardine.list(anyString())).thenReturn(resources);

            client = WebDavFileTransferClient.builder()
                .baseUrl("https://webdav.example.com/")
                .username("user")
                .password("pass")
                .build();

            assertEquals(12345L, client.getFileSize("test.txt"));
        }
    }

    @Test
    @DisplayName("Should throw IOException when file not found for size")
    void testGetFileSizeNotFound() throws Exception {
        try (MockedStatic<SardineFactory> factoryMock = mockStatic(SardineFactory.class)) {
            Sardine sardine = mock(Sardine.class);
            factoryMock.when(() -> SardineFactory.begin()).thenReturn(sardine);
            when(sardine.list(anyString())).thenReturn(Collections.emptyList());

            client = WebDavFileTransferClient.builder()
                .baseUrl("https://webdav.example.com/")
                .build();

            IOException thrown = assertThrows(IOException.class, () -> client.getFileSize("missing.txt"));
            assertTrue(thrown.getMessage().contains("File not found"));
        }
    }

    @Test
    @DisplayName("Should throw IOException when getting size of directory")
    void testGetFileSizeDirectory() throws Exception {
        try (MockedStatic<SardineFactory> factoryMock = mockStatic(SardineFactory.class)) {
            Sardine sardine = mock(Sardine.class);
            factoryMock.when(() -> SardineFactory.begin()).thenReturn(sardine);

            DavResource resource = mock(DavResource.class);
            when(resource.isDirectory()).thenReturn(true);

            List<DavResource> resources = Collections.singletonList(resource);
            when(sardine.list(anyString())).thenReturn(resources);

            client = WebDavFileTransferClient.builder()
                .baseUrl("https://webdav.example.com/")
                .build();

            IOException thrown = assertThrows(IOException.class, () -> client.getFileSize("directory"));
            assertTrue(thrown.getMessage().contains("Path is a directory"));
        }
    }

    @Test
    @DisplayName("Should return 0 when content length is null")
    void testGetFileSizeNullContentLength() throws Exception {
        try (MockedStatic<SardineFactory> factoryMock = mockStatic(SardineFactory.class)) {
            Sardine sardine = mock(Sardine.class);
            factoryMock.when(() -> SardineFactory.begin(anyString(), anyString())).thenReturn(sardine);

            DavResource resource = mock(DavResource.class);
            when(resource.isDirectory()).thenReturn(false);
            when(resource.getContentLength()).thenReturn(null);

            List<DavResource> resources = Collections.singletonList(resource);
            when(sardine.list(anyString())).thenReturn(resources);

            client = WebDavFileTransferClient.builder()
                .baseUrl("https://webdav.example.com/")
                .username("user")
                .password("pass")
                .build();

            assertEquals(0L, client.getFileSize("test.txt"));
        }
    }

    @Test
    @DisplayName("Should close and shutdown Sardine")
    void testClose() throws Exception {
        try (MockedStatic<SardineFactory> factoryMock = mockStatic(SardineFactory.class)) {
            Sardine sardine = mock(Sardine.class);
            factoryMock.when(() -> SardineFactory.begin()).thenReturn(sardine);
            doNothing().when(sardine).shutdown();

            client = WebDavFileTransferClient.builder()
                .baseUrl("https://webdav.example.com/")
                .build();

            client.close();

            verify(sardine).shutdown();
        }
    }

    @Test
    @DisplayName("Should propagate IOException from shutdown")
    void testCloseError() throws Exception {
        try (MockedStatic<SardineFactory> factoryMock = mockStatic(SardineFactory.class)) {
            Sardine sardine = mock(Sardine.class);
            factoryMock.when(() -> SardineFactory.begin(anyString(), anyString())).thenReturn(sardine);
            doThrow(new IOException("Shutdown failed")).when(sardine).shutdown();

            client = WebDavFileTransferClient.builder()
                .baseUrl("https://webdav.example.com/")
                .username("user")
                .password("pass")
                .build();

            IOException thrown = assertThrows(IOException.class, () -> client.close());
            assertTrue(thrown.getMessage().contains("Shutdown failed"));
        }
    }

    private WebDavFileTransferClient createTestClient(String baseUrl, String username, String password) throws Exception {
        java.lang.reflect.Constructor<WebDavFileTransferClient> constructor =
            WebDavFileTransferClient.class.getDeclaredConstructor(
                String.class, String.class, String.class);
        constructor.setAccessible(true);

        return constructor.newInstance(baseUrl, username, password);
    }
}
