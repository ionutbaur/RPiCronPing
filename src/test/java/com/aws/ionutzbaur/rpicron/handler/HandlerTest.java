package com.aws.ionutzbaur.rpicron.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.aws.ionutzbaur.rpicron.datasource.ScanNotification;
import com.aws.ionutzbaur.rpicron.model.Notification;
import com.aws.ionutzbaur.rpicron.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.io.IOException;
import java.net.Socket;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandlerTest {

    private static final String BUCHAREST_ZONE_ID = "Europe/Bucharest";
    private static final int ANY_NOT_IGNORED_HOUR = 12;

    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    @Mock
    private ZonedDateTime zonedDateTime;

    @Mock
    private ScanNotification scanNotification;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DynamoDbTable<Notification> notificationTable;

    @Spy
    private Notification notification;

    private MockedStatic<ZonedDateTime> zonedDateTimeMockedStatic;

    private Handler handler;

    @BeforeEach
    void setUp() {
        when(context.getLogger()).thenReturn(logger);

        zonedDateTimeMockedStatic = mockStatic(ZonedDateTime.class);
        zonedDateTimeMockedStatic.when(ZonedDateTime::now).thenReturn(zonedDateTime);
        when(zonedDateTime.withZoneSameInstant(ZoneId.of(BUCHAREST_ZONE_ID))).thenReturn(zonedDateTime);

        handler = new Handler(scanNotification, notificationService);
    }

    @AfterEach
    void tearDown() {
        zonedDateTimeMockedStatic.close();
    }

    @Test
    void handleRequest_isIgnoredInterval() {
        when(zonedDateTime.getHour()).thenReturn(3);
        when(zonedDateTime.getMinute()).thenReturn(1);

        handler.handleRequest(context);

        verify(logger).log("Interval ignored. Will do nothing.");
        verifyNoMoreInteractions(logger, scanNotification, notificationService);
    }

    @Test
    void handleRequest_isAlreadyAlive() {
        when(zonedDateTime.getHour()).thenReturn(ANY_NOT_IGNORED_HOUR);
        when(scanNotification.getNotification(logger)).thenReturn(notification);
        when(notification.getAlive()).thenReturn(true);

        try (MockedConstruction<Socket> ignored = mockConstruction(Socket.class)) {
            handler.handleRequest(context);

            verify(logger).log("Raspberry Pi is alive. No action needed.");
            verifyNoMoreInteractions(logger, scanNotification, notificationService);
        }
    }

    @Test
    void handleRequest_isBackAlive() {
        when(zonedDateTime.getHour()).thenReturn(ANY_NOT_IGNORED_HOUR);
        when(scanNotification.getNotification(logger)).thenReturn(notification);
        when(notification.getAlive())
                .thenReturn(false)  // first mock to false
                .thenCallRealMethod(); // next, use real result (needed for assertion)
        when(scanNotification.getTable()).thenReturn(notificationTable);
        when(notificationService.sendNotification("Raspberry Pi is now reachable!", logger)).thenReturn(true);

        try (MockedConstruction<Socket> ignored = mockConstruction(Socket.class)) {
            handler.handleRequest(context);

            verify(logger).log("Raspberry Pi back alive! Will try to notify user.");

            assertTrue(notification.getAlive());
            assertEquals(zonedDateTime.toString(), notification.getLastSentOn());
            assertTrue(notification.getNextNotificationNeeded());
            verify(notificationTable).updateItem(notification);

            verify(logger).log("Successfully sent notification!");
        }
    }

    @Test
    void handleRequest_isDown_notificationNeeded() {
        when(zonedDateTime.getHour()).thenReturn(ANY_NOT_IGNORED_HOUR);
        when(scanNotification.getNotification(logger)).thenReturn(notification);
        when(notification.getNextNotificationNeeded())
                .thenReturn(true)   // first mock to true
                .thenCallRealMethod();  // next, use real result (needed for assertion)
        when(scanNotification.getTable()).thenReturn(notificationTable);
        when(notificationService.sendNotification("Raspberry Pi not reachable! Most probably a power outage.", logger))
                .thenReturn(true);

        try (MockedConstruction<Socket> ignored = mockConstruction(Socket.class,
                (socket, contextOfMock) -> when(contextOfMock.constructor().newInstance())
                        .thenThrow(IOException.class))) {
            handler.handleRequest(context);

            verify(logger).log(matches("Cannot connect to host due to *."));
            verify(logger).log("Raspberry Pi down. Will try to notify user.");

            assertFalse(notification.getAlive());
            assertEquals(zonedDateTime.toString(), notification.getLastSentOn());
            assertFalse(notification.getNextNotificationNeeded());
            verify(notificationTable).updateItem(notification);

            verify(logger).log("Successfully sent notification!");
        }
    }

    @Test
    void handleRequest_stillDown_notificationNotNeeded() {
        when(zonedDateTime.getHour()).thenReturn(ANY_NOT_IGNORED_HOUR);
        when(scanNotification.getNotification(logger)).thenReturn(notification);
        when(notification.getNextNotificationNeeded()).thenReturn(false);

        try (MockedConstruction<Socket> ignored = mockConstruction(Socket.class,
                (socket, contextOfMock) -> when(contextOfMock.constructor().newInstance())
                        .thenThrow(IOException.class))) {
            handler.handleRequest(context);

            verify(logger).log("Raspberry Pi still down. No further notification needed.");
            verifyNoMoreInteractions(logger, scanNotification, notificationService);
        }
    }

}