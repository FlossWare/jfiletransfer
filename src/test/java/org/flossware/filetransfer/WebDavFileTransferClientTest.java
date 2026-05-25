package org.flossware.filetransfer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

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

    private WebDavFileTransferClient createTestClient(String baseUrl, String username, String password) throws Exception {
        java.lang.reflect.Constructor<WebDavFileTransferClient> constructor =
            WebDavFileTransferClient.class.getDeclaredConstructor(
                String.class, String.class, String.class);
        constructor.setAccessible(true);

        return constructor.newInstance(baseUrl, username, password);
    }
}
