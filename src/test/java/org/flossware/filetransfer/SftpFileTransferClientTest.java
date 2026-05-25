package org.flossware.filetransfer;

import com.jcraft.jsch.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.MockedConstruction;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for SftpFileTransferClient to achieve 100% coverage.
 * Note: Most methods require JSch connection which needs a live SFTP server or complex mocking.
 * These tests focus on builder validation, configuration, and path resolution logic.
 */
class SftpFileTransferClientTest {

    private SftpFileTransferClient client;

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
        SftpFileTransferClient.Builder builder = SftpFileTransferClient.builder();
        assertSame(builder, builder.host("example.com"));
        assertSame(builder, builder.port(22));
        assertSame(builder, builder.username("user"));
        assertSame(builder, builder.password("pass"));
        assertSame(builder, builder.privateKey("/path/to/key"));
        assertSame(builder, builder.basePath("/base"));
        assertSame(builder, builder.knownHostsFile("/path/to/known_hosts"));
        assertSame(builder, builder.strictHostKeyChecking(true));
    }

    @Test
    @DisplayName("Should throw NullPointerException when host is null")
    void testBuilderNullHost() {
        assertThrows(NullPointerException.class,
            () -> SftpFileTransferClient.builder()
                .username("user")
                .password("pass")
                .build());
    }

    @Test
    @DisplayName("Should throw NullPointerException when username is null")
    void testBuilderNullUsername() {
        assertThrows(NullPointerException.class,
            () -> SftpFileTransferClient.builder()
                .host("example.com")
                .password("pass")
                .build());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when both password and privateKey are null")
    void testBuilderMissingAuth() {
        assertThrows(IllegalStateException.class,
            () -> SftpFileTransferClient.builder()
                .host("example.com")
                .username("user")
                .build());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for port less than 1")
    void testBuilderInvalidPortTooLow() {
        assertThrows(IllegalArgumentException.class,
            () -> SftpFileTransferClient.builder()
                .host("example.com")
                .port(0)
                .username("user")
                .password("pass")
                .build());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for port greater than 65535")
    void testBuilderInvalidPortTooHigh() {
        assertThrows(IllegalArgumentException.class,
            () -> SftpFileTransferClient.builder()
                .host("example.com")
                .port(65536)
                .username("user")
                .password("pass")
                .build());
    }

    @Test
    @DisplayName("Should build client with password authentication")
    void testBuildWithPassword() throws Exception {
        client = createTestClient("pass", null, null);
        assertNotNull(client);
        String description = client.getDescription();
        assertTrue(description.contains("SFTP["));
        assertTrue(description.contains("user@example.com:22"));
    }

    @Test
    @DisplayName("Should build client with private key authentication")
    void testBuildWithPrivateKey() throws Exception {
        client = createTestClient(null, "/path/to/key", null);
        assertNotNull(client);
        String description = client.getDescription();
        assertTrue(description.contains("SFTP["));
        assertTrue(description.contains("user@example.com:22"));
    }

    @Test
    @DisplayName("Should build client with base path")
    void testBuildWithBasePath() throws Exception {
        client = createTestClient("pass", null, "/base");
        assertNotNull(client);
        String description = client.getDescription();
        assertTrue(description.contains("/base]"));
    }

    @Test
    @DisplayName("Should build client with default port 22")
    void testBuildWithDefaultPort() throws Exception {
        client = SftpFileTransferClient.builder()
            .host("example.com")
            .username("user")
            .password("pass")
            .build();

        assertNotNull(client);
        assertTrue(client.getDescription().contains(":22"));
    }

    @Test
    @DisplayName("Should build client with custom port")
    void testBuildWithCustomPort() throws Exception {
        client = createTestClientWithPort(2222);
        assertNotNull(client);
        assertTrue(client.getDescription().contains(":2222"));
    }

    @Test
    @DisplayName("Should resolve path without base path")
    void testResolvePathWithoutBasePath() throws Exception {
        client = createTestClient("pass", null, "");

        Method resolvePath = SftpFileTransferClient.class.getDeclaredMethod("resolvePath", String.class);
        resolvePath.setAccessible(true);

        String result = (String) resolvePath.invoke(client, "test.txt");
        assertEquals("/test.txt", result);
    }

    @Test
    @DisplayName("Should resolve path with base path")
    void testResolvePathWithBasePath() throws Exception {
        client = createTestClient("pass", null, "/base");

        Method resolvePath = SftpFileTransferClient.class.getDeclaredMethod("resolvePath", String.class);
        resolvePath.setAccessible(true);

        String result = (String) resolvePath.invoke(client, "test.txt");
        assertEquals("/base/test.txt", result);
    }

    @Test
    @DisplayName("Should resolve path with trailing slash in base path")
    void testResolvePathTrailingSlash() throws Exception {
        client = createTestClient("pass", null, "/base/");

        Method resolvePath = SftpFileTransferClient.class.getDeclaredMethod("resolvePath", String.class);
        resolvePath.setAccessible(true);

        String result = (String) resolvePath.invoke(client, "test.txt");
        assertEquals("/base/test.txt", result);
    }

    @Test
    @DisplayName("Should resolve path with multiple slashes")
    void testResolvePathMultipleSlashes() throws Exception {
        client = createTestClient("pass", null, "/base/");

        Method resolvePath = SftpFileTransferClient.class.getDeclaredMethod("resolvePath", String.class);
        resolvePath.setAccessible(true);

        String result = (String) resolvePath.invoke(client, "/test.txt");
        // Should handle leading slash in filename
        assertTrue(result.equals("/base//test.txt") || result.equals("/base/test.txt"));
    }

    @Test
    @DisplayName("Should return description with password auth")
    void testGetDescriptionPassword() throws Exception {
        client = createTestClient("pass", null, "/base");

        String description = client.getDescription();
        assertTrue(description.contains("SFTP["));
        assertTrue(description.contains("user@example.com:22"));
        assertTrue(description.contains("/base]"));
    }

    @Test
    @DisplayName("Should return description with private key auth")
    void testGetDescriptionPrivateKey() throws Exception {
        client = createTestClient(null, "/path/to/key", "");

        String description = client.getDescription();
        assertTrue(description.contains("SFTP["));
        assertTrue(description.contains("user@example.com:22"));
    }

    @Test
    @DisplayName("Should close without error when not connected")
    void testCloseNotConnected() throws Exception {
        client = createTestClient("pass", null, null);

        assertDoesNotThrow(() -> client.close());
    }

    @Test
    @DisplayName("Should throw NullPointerException when constructor receives null host")
    void testConstructorNullHost() throws Exception {
        java.lang.reflect.Constructor<SftpFileTransferClient> constructor =
            SftpFileTransferClient.class.getDeclaredConstructor(
                String.class, int.class, String.class, String.class, String.class,
                String.class, String.class, boolean.class);
        constructor.setAccessible(true);

        java.lang.reflect.InvocationTargetException exception = assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> constructor.newInstance(null, 22, "user", "pass", null, null, null, false));

        assertTrue(exception.getCause() instanceof NullPointerException);
        assertTrue(exception.getCause().getMessage().contains("host cannot be null"));
    }

    @Test
    @DisplayName("Should throw NullPointerException when constructor receives null username")
    void testConstructorNullUsername() throws Exception {
        java.lang.reflect.Constructor<SftpFileTransferClient> constructor =
            SftpFileTransferClient.class.getDeclaredConstructor(
                String.class, int.class, String.class, String.class, String.class,
                String.class, String.class, boolean.class);
        constructor.setAccessible(true);

        java.lang.reflect.InvocationTargetException exception = assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> constructor.newInstance("example.com", 22, null, "pass", null, null, null, false));

        assertTrue(exception.getCause() instanceof NullPointerException);
        assertTrue(exception.getCause().getMessage().contains("username cannot be null"));
    }

    @Test
    @DisplayName("Should verify DEFAULT_SESSION_TIMEOUT_MS constant")
    void testConstantSessionTimeout() throws Exception {
        java.lang.reflect.Field sessionTimeout = SftpFileTransferClient.class.getDeclaredField("DEFAULT_SESSION_TIMEOUT_MS");
        sessionTimeout.setAccessible(true);
        assertEquals(30000, sessionTimeout.get(null));
    }

    @Test
    @DisplayName("Should verify DEFAULT_CHANNEL_TIMEOUT_MS constant")
    void testConstantChannelTimeout() throws Exception {
        java.lang.reflect.Field channelTimeout = SftpFileTransferClient.class.getDeclaredField("DEFAULT_CHANNEL_TIMEOUT_MS");
        channelTimeout.setAccessible(true);
        assertEquals(10000, channelTimeout.get(null));
    }

    @Test
    @DisplayName("Should verify DEFAULT_BUFFER_SIZE constant")
    void testConstantBufferSize() throws Exception {
        java.lang.reflect.Field bufferSize = SftpFileTransferClient.class.getDeclaredField("DEFAULT_BUFFER_SIZE");
        bufferSize.setAccessible(true);
        assertEquals(8192, bufferSize.get(null));
    }

    @Test
    @DisplayName("Should configure strict host key checking enabled")
    void testStrictHostKeyCheckingEnabled() throws Exception {
        client = SftpFileTransferClient.builder()
            .host("example.com")
            .username("user")
            .password("pass")
            .strictHostKeyChecking(true)
            .build();

        assertNotNull(client);
    }

    @Test
    @DisplayName("Should configure strict host key checking disabled")
    void testStrictHostKeyCheckingDisabled() throws Exception {
        client = SftpFileTransferClient.builder()
            .host("example.com")
            .username("user")
            .password("pass")
            .strictHostKeyChecking(false)
            .build();

        assertNotNull(client);
    }

    @Test
    @DisplayName("Should configure known hosts file")
    void testKnownHostsFile() throws Exception {
        client = SftpFileTransferClient.builder()
            .host("example.com")
            .username("user")
            .password("pass")
            .knownHostsFile("/path/to/known_hosts")
            .build();

        assertNotNull(client);
    }

    @Test
    @DisplayName("Should accept valid port 1")
    void testValidPortMinimum() throws Exception {
        client = SftpFileTransferClient.builder()
            .host("example.com")
            .port(1)
            .username("user")
            .password("pass")
            .build();

        assertNotNull(client);
        assertTrue(client.getDescription().contains(":1"));
    }

    @Test
    @DisplayName("Should accept valid port 65535")
    void testValidPortMaximum() throws Exception {
        client = SftpFileTransferClient.builder()
            .host("example.com")
            .port(65535)
            .username("user")
            .password("pass")
            .build();

        assertNotNull(client);
        assertTrue(client.getDescription().contains(":65535"));
    }

    @Test
    @DisplayName("Should handle null basePath in constructor")
    void testConstructorNullBasePath() throws Exception {
        client = createTestClient("pass", null, null);
        assertNotNull(client);
        // Null basePath should not cause NPE
        String description = client.getDescription();
        assertTrue(description.contains("SFTP["));
    }

    @Test
    @DisplayName("Should handle both password and privateKey null in constructor")
    void testConstructorBothAuthNull() throws Exception {
        java.lang.reflect.Constructor<SftpFileTransferClient> constructor =
            SftpFileTransferClient.class.getDeclaredConstructor(
                String.class, int.class, String.class, String.class, String.class,
                String.class, String.class, boolean.class);
        constructor.setAccessible(true);

        SftpFileTransferClient testClient = constructor.newInstance(
            "example.com", 22, "user", null, null, null, null, false);
        assertNotNull(testClient);
        testClient.close();
    }

    // Tests for actual SFTP operations using mocked JSch components

    @Test
    @DisplayName("Should read file successfully")
    void testReadFileSuccess() throws Exception {
        byte[] fileContent = "test file content".getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent);

        try (MockedConstruction<JSch> jschMock = mockConstruction(JSch.class, (mock, context) -> {
            Session session = mock(Session.class);
            ChannelSftp channel = mock(ChannelSftp.class);

            when(mock.getSession(anyString(), anyString(), anyInt())).thenReturn(session);
            doNothing().when(session).setPassword(anyString());
            doNothing().when(session).setConfig(any(Properties.class));
            doNothing().when(session).connect(anyInt());
            when(session.openChannel("sftp")).thenReturn(channel);
            doNothing().when(channel).connect(anyInt());
            when(channel.isConnected()).thenReturn(true);
            when(channel.get(anyString())).thenReturn(inputStream);
        })) {
            client = SftpFileTransferClient.builder()
                .host("example.com")
                .username("user")
                .password("pass")
                .basePath("/base")
                .build();

            byte[] result = client.readFile("test.txt");
            assertArrayEquals(fileContent, result);
        }
    }

    @Test
    @DisplayName("Should throw IOException when readFile fails")
    void testReadFileFailure() throws Exception {
        try (MockedConstruction<JSch> jschMock = mockConstruction(JSch.class, (mock, context) -> {
            Session session = mock(Session.class);
            ChannelSftp channel = mock(ChannelSftp.class);

            when(mock.getSession(anyString(), anyString(), anyInt())).thenReturn(session);
            doNothing().when(session).setPassword(anyString());
            doNothing().when(session).setConfig(any(Properties.class));
            doNothing().when(session).connect(anyInt());
            when(session.openChannel("sftp")).thenReturn(channel);
            doNothing().when(channel).connect(anyInt());
            when(channel.isConnected()).thenReturn(true);
            when(channel.get(anyString())).thenThrow(new SftpException(0, "File not found"));
        })) {
            client = SftpFileTransferClient.builder()
                .host("example.com")
                .username("user")
                .password("pass")
                .build();

            IOException thrown = assertThrows(IOException.class, () -> client.readFile("missing.txt"));
            assertTrue(thrown.getMessage().contains("Failed to read file from SFTP"));
        }
    }

    @Test
    @DisplayName("Should open file successfully")
    void testOpenFileSuccess() throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream("test".getBytes());

        try (MockedConstruction<JSch> jschMock = mockConstruction(JSch.class, (mock, context) -> {
            Session session = mock(Session.class);
            ChannelSftp channel = mock(ChannelSftp.class);

            when(mock.getSession(anyString(), anyString(), anyInt())).thenReturn(session);
            doNothing().when(session).setPassword(anyString());
            doNothing().when(session).setConfig(any(Properties.class));
            doNothing().when(session).connect(anyInt());
            when(session.openChannel("sftp")).thenReturn(channel);
            doNothing().when(channel).connect(anyInt());
            when(channel.isConnected()).thenReturn(true);
            when(channel.get(anyString())).thenReturn(inputStream);
        })) {
            client = SftpFileTransferClient.builder()
                .host("example.com")
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
        try (MockedConstruction<JSch> jschMock = mockConstruction(JSch.class, (mock, context) -> {
            Session session = mock(Session.class);
            ChannelSftp channel = mock(ChannelSftp.class);

            when(mock.getSession(anyString(), anyString(), anyInt())).thenReturn(session);
            doNothing().when(session).setPassword(anyString());
            doNothing().when(session).setConfig(any(Properties.class));
            doNothing().when(session).connect(anyInt());
            when(session.openChannel("sftp")).thenReturn(channel);
            doNothing().when(channel).connect(anyInt());
            when(channel.isConnected()).thenReturn(true);
            when(channel.get(anyString())).thenThrow(new SftpException(0, "Cannot open file"));
        })) {
            client = SftpFileTransferClient.builder()
                .host("example.com")
                .username("user")
                .password("pass")
                .build();

            IOException thrown = assertThrows(IOException.class, () -> client.openFile("test.txt"));
            assertTrue(thrown.getMessage().contains("Failed to open file from SFTP"));
        }
    }

    @Test
    @DisplayName("Should return true when file exists")
    void testExistsTrue() throws Exception {
        try (MockedConstruction<JSch> jschMock = mockConstruction(JSch.class, (mock, context) -> {
            Session session = mock(Session.class);
            ChannelSftp channel = mock(ChannelSftp.class);
            SftpATTRS attrs = mock(SftpATTRS.class);

            when(mock.getSession(anyString(), anyString(), anyInt())).thenReturn(session);
            doNothing().when(session).setPassword(anyString());
            doNothing().when(session).setConfig(any(Properties.class));
            doNothing().when(session).connect(anyInt());
            when(session.openChannel("sftp")).thenReturn(channel);
            doNothing().when(channel).connect(anyInt());
            when(channel.isConnected()).thenReturn(true);
            when(channel.stat(anyString())).thenReturn(attrs);
            when(attrs.isDir()).thenReturn(false);
        })) {
            client = SftpFileTransferClient.builder()
                .host("example.com")
                .username("user")
                .password("pass")
                .build();

            assertTrue(client.exists("test.txt"));
        }
    }

    @Test
    @DisplayName("Should return false when file does not exist")
    void testExistsFalse() throws Exception {
        try (MockedConstruction<JSch> jschMock = mockConstruction(JSch.class, (mock, context) -> {
            Session session = mock(Session.class);
            ChannelSftp channel = mock(ChannelSftp.class);

            when(mock.getSession(anyString(), anyString(), anyInt())).thenReturn(session);
            doNothing().when(session).setPassword(anyString());
            doNothing().when(session).setConfig(any(Properties.class));
            doNothing().when(session).connect(anyInt());
            when(session.openChannel("sftp")).thenReturn(channel);
            doNothing().when(channel).connect(anyInt());
            when(channel.isConnected()).thenReturn(true);
            when(channel.stat(anyString())).thenThrow(new SftpException(ChannelSftp.SSH_FX_NO_SUCH_FILE, "No such file"));
        })) {
            client = SftpFileTransferClient.builder()
                .host("example.com")
                .username("user")
                .password("pass")
                .build();

            assertFalse(client.exists("missing.txt"));
        }
    }

    @Test
    @DisplayName("Should return false when path is directory")
    void testExistsDirectory() throws Exception {
        try (MockedConstruction<JSch> jschMock = mockConstruction(JSch.class, (mock, context) -> {
            Session session = mock(Session.class);
            ChannelSftp channel = mock(ChannelSftp.class);
            SftpATTRS attrs = mock(SftpATTRS.class);

            when(mock.getSession(anyString(), anyString(), anyInt())).thenReturn(session);
            doNothing().when(session).setPassword(anyString());
            doNothing().when(session).setConfig(any(Properties.class));
            doNothing().when(session).connect(anyInt());
            when(session.openChannel("sftp")).thenReturn(channel);
            doNothing().when(channel).connect(anyInt());
            when(channel.isConnected()).thenReturn(true);
            when(channel.stat(anyString())).thenReturn(attrs);
            when(attrs.isDir()).thenReturn(true);
        })) {
            client = SftpFileTransferClient.builder()
                .host("example.com")
                .username("user")
                .password("pass")
                .build();

            assertFalse(client.exists("directory"));
        }
    }

    @Test
    @DisplayName("Should throw IOException when exists check fails with unexpected error")
    void testExistsError() throws Exception {
        try (MockedConstruction<JSch> jschMock = mockConstruction(JSch.class, (mock, context) -> {
            Session session = mock(Session.class);
            ChannelSftp channel = mock(ChannelSftp.class);

            when(mock.getSession(anyString(), anyString(), anyInt())).thenReturn(session);
            doNothing().when(session).setPassword(anyString());
            doNothing().when(session).setConfig(any(Properties.class));
            doNothing().when(session).connect(anyInt());
            when(session.openChannel("sftp")).thenReturn(channel);
            doNothing().when(channel).connect(anyInt());
            when(channel.isConnected()).thenReturn(true);
            when(channel.stat(anyString())).thenThrow(new SftpException(5, "Permission denied"));
        })) {
            client = SftpFileTransferClient.builder()
                .host("example.com")
                .username("user")
                .password("pass")
                .build();

            IOException thrown = assertThrows(IOException.class, () -> client.exists("test.txt"));
            assertTrue(thrown.getMessage().contains("Failed to check file existence"));
        }
    }

    @Test
    @DisplayName("Should list files successfully")
    void testListSuccess() throws Exception {
        try (MockedConstruction<JSch> jschMock = mockConstruction(JSch.class, (mock, context) -> {
            Session session = mock(Session.class);
            ChannelSftp channel = mock(ChannelSftp.class);

            Vector<ChannelSftp.LsEntry> entries = new Vector<>();
            ChannelSftp.LsEntry entry1 = mock(ChannelSftp.LsEntry.class);
            ChannelSftp.LsEntry entry2 = mock(ChannelSftp.LsEntry.class);
            ChannelSftp.LsEntry dotEntry = mock(ChannelSftp.LsEntry.class);
            ChannelSftp.LsEntry dotDotEntry = mock(ChannelSftp.LsEntry.class);

            when(entry1.getFilename()).thenReturn("file1.txt");
            when(entry2.getFilename()).thenReturn("file2.txt");
            when(dotEntry.getFilename()).thenReturn(".");
            when(dotDotEntry.getFilename()).thenReturn("..");

            entries.add(dotEntry);
            entries.add(dotDotEntry);
            entries.add(entry1);
            entries.add(entry2);

            when(mock.getSession(anyString(), anyString(), anyInt())).thenReturn(session);
            doNothing().when(session).setPassword(anyString());
            doNothing().when(session).setConfig(any(Properties.class));
            doNothing().when(session).connect(anyInt());
            when(session.openChannel("sftp")).thenReturn(channel);
            doNothing().when(channel).connect(anyInt());
            when(channel.isConnected()).thenReturn(true);
            when(channel.ls(anyString())).thenReturn(entries);
        })) {
            client = SftpFileTransferClient.builder()
                .host("example.com")
                .username("user")
                .password("pass")
                .basePath("/base")
                .build();

            List<String> result = client.list("uploads");
            assertEquals(2, result.size());
            assertTrue(result.contains("uploads/file1.txt"));
            assertTrue(result.contains("uploads/file2.txt"));
        }
    }

    @Test
    @DisplayName("Should list files with trailing slash in prefix")
    void testListWithTrailingSlash() throws Exception {
        try (MockedConstruction<JSch> jschMock = mockConstruction(JSch.class, (mock, context) -> {
            Session session = mock(Session.class);
            ChannelSftp channel = mock(ChannelSftp.class);

            Vector<ChannelSftp.LsEntry> entries = new Vector<>();
            ChannelSftp.LsEntry entry = mock(ChannelSftp.LsEntry.class);
            when(entry.getFilename()).thenReturn("file.txt");
            entries.add(entry);

            when(mock.getSession(anyString(), anyString(), anyInt())).thenReturn(session);
            doNothing().when(session).setPassword(anyString());
            doNothing().when(session).setConfig(any(Properties.class));
            doNothing().when(session).connect(anyInt());
            when(session.openChannel("sftp")).thenReturn(channel);
            doNothing().when(channel).connect(anyInt());
            when(channel.isConnected()).thenReturn(true);
            when(channel.ls(anyString())).thenReturn(entries);
        })) {
            client = SftpFileTransferClient.builder()
                .host("example.com")
                .username("user")
                .password("pass")
                .build();

            List<String> result = client.list("uploads/");
            assertEquals(1, result.size());
            assertEquals("uploads/file.txt", result.get(0));
        }
    }

    @Test
    @DisplayName("Should throw IOException when list fails")
    void testListFailure() throws Exception {
        try (MockedConstruction<JSch> jschMock = mockConstruction(JSch.class, (mock, context) -> {
            Session session = mock(Session.class);
            ChannelSftp channel = mock(ChannelSftp.class);

            when(mock.getSession(anyString(), anyString(), anyInt())).thenReturn(session);
            doNothing().when(session).setPassword(anyString());
            doNothing().when(session).setConfig(any(Properties.class));
            doNothing().when(session).connect(anyInt());
            when(session.openChannel("sftp")).thenReturn(channel);
            doNothing().when(channel).connect(anyInt());
            when(channel.isConnected()).thenReturn(true);
            when(channel.ls(anyString())).thenThrow(new SftpException(0, "Directory not found"));
        })) {
            client = SftpFileTransferClient.builder()
                .host("example.com")
                .username("user")
                .password("pass")
                .build();

            IOException thrown = assertThrows(IOException.class, () -> client.list("missing"));
            assertTrue(thrown.getMessage().contains("Failed to list files from SFTP"));
        }
    }

    @Test
    @DisplayName("Should get file size successfully")
    void testGetFileSizeSuccess() throws Exception {
        try (MockedConstruction<JSch> jschMock = mockConstruction(JSch.class, (mock, context) -> {
            Session session = mock(Session.class);
            ChannelSftp channel = mock(ChannelSftp.class);
            SftpATTRS attrs = mock(SftpATTRS.class);

            when(mock.getSession(anyString(), anyString(), anyInt())).thenReturn(session);
            doNothing().when(session).setPassword(anyString());
            doNothing().when(session).setConfig(any(Properties.class));
            doNothing().when(session).connect(anyInt());
            when(session.openChannel("sftp")).thenReturn(channel);
            doNothing().when(channel).connect(anyInt());
            when(channel.isConnected()).thenReturn(true);
            when(channel.stat(anyString())).thenReturn(attrs);
            when(attrs.isDir()).thenReturn(false);
            when(attrs.getSize()).thenReturn(12345L);
        })) {
            client = SftpFileTransferClient.builder()
                .host("example.com")
                .username("user")
                .password("pass")
                .build();

            assertEquals(12345L, client.getFileSize("test.txt"));
        }
    }

    @Test
    @DisplayName("Should throw IOException when getting size of directory")
    void testGetFileSizeDirectory() throws Exception {
        try (MockedConstruction<JSch> jschMock = mockConstruction(JSch.class, (mock, context) -> {
            Session session = mock(Session.class);
            ChannelSftp channel = mock(ChannelSftp.class);
            SftpATTRS attrs = mock(SftpATTRS.class);

            when(mock.getSession(anyString(), anyString(), anyInt())).thenReturn(session);
            doNothing().when(session).setPassword(anyString());
            doNothing().when(session).setConfig(any(Properties.class));
            doNothing().when(session).connect(anyInt());
            when(session.openChannel("sftp")).thenReturn(channel);
            doNothing().when(channel).connect(anyInt());
            when(channel.isConnected()).thenReturn(true);
            when(channel.stat(anyString())).thenReturn(attrs);
            when(attrs.isDir()).thenReturn(true);
        })) {
            client = SftpFileTransferClient.builder()
                .host("example.com")
                .username("user")
                .password("pass")
                .build();

            IOException thrown = assertThrows(IOException.class, () -> client.getFileSize("directory"));
            assertTrue(thrown.getMessage().contains("Path is not a file"));
        }
    }

    @Test
    @DisplayName("Should throw IOException when getFileSize fails")
    void testGetFileSizeFailure() throws Exception {
        try (MockedConstruction<JSch> jschMock = mockConstruction(JSch.class, (mock, context) -> {
            Session session = mock(Session.class);
            ChannelSftp channel = mock(ChannelSftp.class);

            when(mock.getSession(anyString(), anyString(), anyInt())).thenReturn(session);
            doNothing().when(session).setPassword(anyString());
            doNothing().when(session).setConfig(any(Properties.class));
            doNothing().when(session).connect(anyInt());
            when(session.openChannel("sftp")).thenReturn(channel);
            doNothing().when(channel).connect(anyInt());
            when(channel.isConnected()).thenReturn(true);
            when(channel.stat(anyString())).thenThrow(new SftpException(0, "File not found"));
        })) {
            client = SftpFileTransferClient.builder()
                .host("example.com")
                .username("user")
                .password("pass")
                .build();

            IOException thrown = assertThrows(IOException.class, () -> client.getFileSize("missing.txt"));
            assertTrue(thrown.getMessage().contains("Failed to get file size"));
        }
    }

    @Test
    @DisplayName("Should close connected client successfully")
    void testCloseConnected() throws Exception {
        client = SftpFileTransferClient.builder()
            .host("example.com")
            .username("user")
            .password("pass")
            .build();

        // Inject mocked session and channel to test close
        Session mockSession = mock(Session.class);
        ChannelSftp mockChannel = mock(ChannelSftp.class);

        when(mockSession.isConnected()).thenReturn(true);
        when(mockChannel.isConnected()).thenReturn(true);

        Field sessionField = SftpFileTransferClient.class.getDeclaredField("session");
        sessionField.setAccessible(true);
        sessionField.set(client, mockSession);

        Field channelField = SftpFileTransferClient.class.getDeclaredField("sftpChannel");
        channelField.setAccessible(true);
        channelField.set(client, mockChannel);

        client.close();

        verify(mockChannel).disconnect();
        verify(mockSession).disconnect();
    }

    @Test
    @DisplayName("Should throw IOException when connection fails")
    void testConnectionFailure() throws Exception {
        try (MockedConstruction<JSch> jschMock = mockConstruction(JSch.class, (mock, context) -> {
            when(mock.getSession(anyString(), anyString(), anyInt())).thenThrow(new JSchException("Connection refused"));
        })) {
            client = SftpFileTransferClient.builder()
                .host("example.com")
                .username("user")
                .password("pass")
                .build();

            IOException thrown = assertThrows(IOException.class, () -> client.readFile("test.txt"));
            assertTrue(thrown.getMessage().contains("Failed to connect to SFTP server"));
        }
    }

    @Test
    @DisplayName("Should reuse existing connection")
    void testReuseConnection() throws Exception {
        try (MockedConstruction<JSch> jschMock = mockConstruction(JSch.class, (mock, context) -> {
            Session session = mock(Session.class);
            ChannelSftp channel = mock(ChannelSftp.class);
            ByteArrayInputStream inputStream1 = new ByteArrayInputStream("file1".getBytes());
            ByteArrayInputStream inputStream2 = new ByteArrayInputStream("file2".getBytes());

            when(mock.getSession(anyString(), anyString(), anyInt())).thenReturn(session);
            doNothing().when(session).setPassword(anyString());
            doNothing().when(session).setConfig(any(Properties.class));
            doNothing().when(session).connect(anyInt());
            when(session.openChannel("sftp")).thenReturn(channel);
            doNothing().when(channel).connect(anyInt());
            when(channel.isConnected()).thenReturn(true);
            when(channel.get("/base/file1.txt")).thenReturn(inputStream1);
            when(channel.get("/base/file2.txt")).thenReturn(inputStream2);
        })) {
            client = SftpFileTransferClient.builder()
                .host("example.com")
                .username("user")
                .password("pass")
                .basePath("/base")
                .build();

            client.readFile("file1.txt");
            client.readFile("file2.txt");

            // Should only create one JSch instance (connection reused)
            assertEquals(1, jschMock.constructed().size());
        }
    }

    private SftpFileTransferClient createTestClient(String password, String privateKeyPath, String basePath) throws Exception {
        java.lang.reflect.Constructor<SftpFileTransferClient> constructor =
            SftpFileTransferClient.class.getDeclaredConstructor(
                String.class, int.class, String.class, String.class, String.class,
                String.class, String.class, boolean.class);
        constructor.setAccessible(true);

        return constructor.newInstance(
            "example.com", 22, "user", password, privateKeyPath, basePath, null, false);
    }

    private SftpFileTransferClient createTestClientWithPort(int port) throws Exception {
        java.lang.reflect.Constructor<SftpFileTransferClient> constructor =
            SftpFileTransferClient.class.getDeclaredConstructor(
                String.class, int.class, String.class, String.class, String.class,
                String.class, String.class, boolean.class);
        constructor.setAccessible(true);

        return constructor.newInstance(
            "example.com", port, "user", "pass", null, null, null, false);
    }
}
